# Flujo de Compra-Venta de Tokens

## 1. Modificaciones Implementadas

### 1.1 Eliminación del precio almacenado en `TradeOrder`

**Qué cambió:**
- Se eliminó el campo `price` (BigDecimal) de la entidad `TradeOrder` y su columna en la tabla `trade_orders`.
- Se quitaron los `.price(price)` de los builders de `TradeOrder` tanto en `OrderService` como en los tests.

**Por qué:**
- El precio guardado al crear la orden era una instantánea que nunca se usaba en la lógica de matching ni en los cálculos financieros.
- Toda ejecución usa `quoteService.getCurrentPrice()` al momento de la transacción. Tener un precio duplicado en `TradeOrder` era engañoso: parecía relevante pero no lo era.
- El `Transaction.pricePerToken` ya registra el precio real de ejecución para contabilidad.

### 1.2 Eliminación del backstop de superusuario en ventas

**Qué cambió:**
- Se eliminó el bloque completo de superusuario fallback dentro de `sell()`.

**Por qué:**
- En el flujo de venta, si no hay órdenes de compra pendientes que matcheen, la orden de venta debe quedar `PENDING` en el libro hasta que algún usuario publique una orden de compra. El superusuario no debe comprar tokens de usuarios.
- En el flujo de compra el superusuario **sí** actúa como backstop, vendiendo de su inventario cuando no hay suficientes órdenes de venta.

### 1.3 Eliminación de duplicación de stubs en tests

**Qué cambió:**
- Se eliminaron stubs redundantes en `OrderServiceTest` que causaban `UnnecessaryStubbingException`.

### 1.4 Corrección de doble deducción en superuser fallback de compra

**Qué cambió:**
- Se eliminó la deducción manual de `superuserToken.setQuantity(...)` + `playerTokenRepository.save(superuserToken)` antes de llamar a `transferTokens()`, porque `transferTokens` ya realiza esa operación.

**Por qué:**
- El código estaba deduciendo los tokens del superusuario dos veces: primero manualmente y luego dentro de `transferTokens`, resultando en un saldo incorrecto.

---

## 2. Arquitectura Actual

### 2.1 Precio único del sistema

No existe precio definido por el usuario. Todas las transacciones se ejecutan al precio que devuelve `quoteService.getCurrentPrice()` al inicio del método `@Transactional`.

### 2.2 Cálculo del precio

```
value = baseValue(100) + (score × scaleFactor(500))
```

Donde `score` (0.0–1.0) es una suma ponderada de estadísticas normalizadas del jugador:

| Métrica | Peso | Normalización |
|---------|------|---------------|
| Goles/partido | 25% | min(g/partido ÷ 1.0, 1.0) |
| Asistencias/partido | 15% | min(a/partido ÷ 1.0, 1.0) |
| Disparos/partido | 10% | min(disparos/partido ÷ 5.0, 1.0) |
| Pases clave/partido | 10% | min(pases/partido ÷ 3.0, 1.0) |
| Regates/partido | 10% | min(regates/partido ÷ 3.0, 1.0) |
| Tackles/partido | 10% | min(tackles/partido ÷ 5.0, 1.0) |
| Rating | 20% | min(rating ÷ 10.0, 1.0) |

Si no existe cotización para un jugador, el precio default es **1.00**.

### 2.3 Matching FIFO

Las órdenes pendientes se matchean en orden de creación (`createdAt ASC`), excluyendo órdenes del mismo usuario (no se permite autotrading).

---

## 3. Flujo de Compra

```
USUARIO pide comprar X tokens de un jugador
│
├─ 1. Validaciones
│   ├── Cantidad > 0
│   ├── Jugador existe
│   └── Comprador existe
│
├─ 2. Precio = quoteService.getCurrentPrice(player)
│
├─ 3. Validar saldo: buyer.balance >= precio × cantidad
│   └── Si no → BusinessException
│
├─ 4. Crear orden BUY (TradeOrder, status = PENDING)
│
├─ 5. Matching FIFO contra sells pendientes de OTROS usuarios
│   │
│   ├── Por cada sell pendiente (con PESSIMISTIC_WRITE):
│   │   ├── ejecQty = min(restante, sell.remainingQuantity)
│   │   ├── Actualizar sell.filledQuantity y sell.status
│   │   ├── transferTokens(seller → buyer)
│   │   ├── buyer.balance -= total
│   │   ├── seller.balance += total
│   │   └── 2 Transactions (BUY + SELL)
│   │
│   └── Si remainingQty == 0 → orden FILLED
│
├─ 6. Backstop Superusuario (si remainingQty > 0)
│   │
│   ├── ¿superuserToken.quantity >= remainingQty?
│   │   ├── Sí:
│   │   │   ├── transferTokens(superuser → buyer)
│   │   │   ├── buyer.balance -= total
│   │   │   ├── superuser.balance += total
│   │   │   └── 1 Transaction (BUY)
│   │   └── No → orden queda PENDING
│   │
│   └── Si remainingQty == 0 → orden FILLED
│
└─ Response: OrderResponse con precio actual, filledQty, totalAmount
```

---

## 4. Flujo de Venta

```
USUARIO pide vender X tokens de un jugador
│
├─ 1. Validaciones
│   ├── Cantidad > 0
│   ├── Jugador existe
│   ├── Vendedor existe
│   └── sellerToken.quantity >= X → si no, BusinessException
│
├─ 2. Precio = quoteService.getCurrentPrice(player)
│
├─ 3. Crear orden SELL (TradeOrder, status = PENDING)
│
├─ 4. Matching FIFO contra buys pendientes de OTROS usuarios
│   │
│   ├── Por cada buy pendiente (con PESSIMISTIC_WRITE):
│   │   ├── ejecQty = min(restante, buy.remainingQuantity)
│   │   ├── Actualizar buy.filledQuantity y buy.status
│   │   ├── transferTokens(seller → buyer)
│   │   ├── buyer.balance -= total
│   │   ├── seller.balance += total
│   │   └── 2 Transactions (SELL + BUY)
│   │
│   └── Si remainingQty == 0 → orden FILLED
│
├─ 5. Sin backstop (si remainingQty > 0)
│   └── Orden queda PENDING en el libro
│
└─ Response: OrderResponse con precio actual, filledQty, totalAmount
```

---

## 5. Transferencia de Tokens

```
transferTokens(from, to, player, quantity, price):
│
├── fromToken = find(from) con PESSIMISTIC_WRITE
│   └── fromToken.quantity -= quantity
│   └── save(fromToken)
│
├── toToken = findOrCreate(to)
│   └── toToken.avgBuyPrice = promedioPonderado(
│         toToken.avgBuyPrice, toToken.quantity,
│         price, quantity)
│   └── toToken.quantity += quantity
│   └── save(toToken)
```

---

## 6. Diagrama de Estados de una Orden

```
         ┌──────────┐
         │ CREATED  │  (se persiste TradeOrder)
         └────┬─────┘
              │
              ▼
         ┌──────────┐
    ┌────│ PENDING  │◄────────┐
    │    └────┬─────┘         │
    │         │               │
    │    ┌────┴─────┐         │
    │    │ Matching │         │
    │    └────┬─────┘         │
    │         │               │
    │    ┌────┴──────────┐    │
    │    │ Parcial       │    │
    │    │ (filled < qty)│────┘ (queda PENDING hasta
    │    └────┬──────────┘         nuevo matching)
    │         │
    │    ┌────┴─────┐         ┌──────────┐
    │    │ Completo │         │CANCELLED │
    │    │(filled=  │         │(por dueño│
    │    │   qty)   │         │ o SU)    │
    │    └────┬─────┘         └──────────┘
    │         │
    │         ▼
    │    ┌──────────┐
    └───►│  FILLED  │
         └──────────┘
```

---

## 7. Endpoints

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/orders/buy` | Sí | Comprar tokens (backstop SU si no hay sells) |
| `POST` | `/orders/sell` | Sí | Vender tokens (sin backstop) |
| `POST` | `/orders/{id}/cancel` | Sí | Cancelar orden (propia o cualquiera si SU) |
| `GET` | `/orders/book/{playerId}` | No | Libro de órdenes agregado + precio actual |

---

## 8. Entidades

### `User`
- `balance` (BigDecimal) — billetera del usuario.
- `isSuperuser` (Boolean) — indica si es el superusuario.
- El superusuario inicia con balance = 1.000.000 (configurable).

### `PlayerToken`
- `player`, `user`, `quantity`, `avgBuyPrice`.
- Representa la tenencia de tokens de un usuario para un jugador.
- El superusuario recibe 100 tokens por cada jugador al iniciar (configurable).

### `TradeOrder`
- `user`, `player`, `orderType` (BUY/SELL), `quantity`, `filledQuantity`, `status`, `createdAt`.
- **No tiene campo `price`** — el precio de ejecución siempre viene del sistema al momento del match.

### `Transaction`
- Registro contable de cada ejecución.
- `user`, `player`, `transactionType` (BUY/SELL), `quantity`, `pricePerToken`, `totalAmount`.
- Es el único registro del precio real al que se ejecutó una operación.

---

## 9. Archivos Relevantes

| Archivo | Propósito |
|---------|-----------|
| `service/OrderService.java` | Lógica de compra, venta, cancelación y libro de órdenes |
| `service/QuoteService.java` | Obtención y recálculo de cotizaciones |
| `strategy/PerformanceBasedStrategy.java` | Estrategia activa de valuación de jugadores |
| `model/TradeOrder.java` | Entidad de órdenes (sin price) |
| `web/OrderController.java` | Endpoints REST de órdenes |
| `dto/BuyOrderRequest.java` | Request de compra (playerId, buyerId, quantity) |
| `dto/SellOrderRequest.java` | Request de venta (playerId, sellerId, quantity) |
| `dto/OrderResponse.java` | Respuesta con orderId, filledQty, status, price, totalAmount |
| `dto/OrderBookResponse.java` | Libro agregado: cantidades BUY/SELL + precio actual |
| `init/DataInitializer.java` | Creación de superusuario y asignación inicial de tokens |
