package com.futbol.scraping.web;

import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderBookResponse;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.service.AuthorizationService;
import com.futbol.scraping.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Compra y venta de tokens de jugadores")
public class OrderController {

    private final OrderService orderService;
    private final AuthorizationService authorizationService;

    @PostMapping("/buy")
    @Operation(summary = "Comprar tokens", description = "Compra tokens al precio del sistema. Primero matchea contra ordenes de venta existentes, si no hay suficientes vende del superusuario.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderResponse> buy(@RequestBody BuyOrderRequest request) {
        log.info("POST /orders/buy");
        authorizationService.assertUserMatchesOrSuperuser(request.getBuyerId());
        return ResponseEntity.ok(orderService.buy(request));
    }

    @PostMapping("/sell")
    @Operation(summary = "Vender tokens", description = "Vende tokens al precio del sistema. Primero matchea contra ordenes de compra existentes, si no hay suficientes compra el superusuario.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OrderResponse> sell(@RequestBody SellOrderRequest request) {
        log.info("POST /orders/sell");
        authorizationService.assertUserMatchesOrSuperuser(request.getSellerId());
        return ResponseEntity.ok(orderService.sell(request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar orden", description = "Cancela una orden pendiente. Solo el dueno o el superusuario pueden cancelar.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable Long id, @RequestParam Long userId) {
        log.info("POST /orders/{}/cancel by user={}", id, userId);
        authorizationService.assertUserMatchesOrSuperuser(userId);
        orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(Map.of("message", "Order cancelled"));
    }

    @GetMapping("/book/{playerId}")
    @Operation(summary = "Libro de ordenes", description = "Obtiene el libro de ordenes de un jugador con el precio actual del sistema.")
    public ResponseEntity<OrderBookResponse> book(@PathVariable Long playerId) {
        log.info("GET /orders/book/{}", playerId);
        return ResponseEntity.ok(orderService.getOrderBook(playerId));
    }
}
