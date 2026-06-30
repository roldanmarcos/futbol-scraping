package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Resultado de una orden de compra o venta")
public class OrderResponse {
    @Schema(description = "Identificador de la orden", example = "5001")
    private Long orderId;
    @Schema(description = "Identificador del jugador", example = "100")
    private Long playerId;
    @Schema(description = "Nombre del jugador", example = "Lionel Messi")
    private String playerName;
    @Schema(description = "Tipo de orden", example = "BUY")
    private String type;
    @Schema(description = "Precio del token (cotizacion del sistema)", example = "125.50")
    private BigDecimal price;
    @Schema(description = "Cantidad solicitada", example = "10")
    private Integer quantity;
    @Schema(description = "Cantidad ejecutada", example = "7")
    private Integer filledQuantity;
    @Schema(description = "Cantidad restante en el libro", example = "3")
    private Integer remainingQuantity;
    @Schema(description = "Estado de la orden", example = "PARTIALLY_FILLED")
    private String status;
    @Schema(description = "Importe total ejecutado", example = "878.50")
    private BigDecimal totalAmount;
}
