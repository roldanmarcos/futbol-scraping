package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Resultado de una orden de compra o venta")
public class OrderResponse {
    @Schema(description = "Identificador de la transacción", example = "9001")
    private Long transactionId;
    @Schema(description = "Identificador del jugador", example = "100")
    private Long playerId;
    @Schema(description = "Nombre del jugador", example = "Lionel Messi")
    private String playerName;
    @Schema(description = "Tipo de orden", example = "BUY")
    private String type;
    @Schema(description = "Cantidad ejecutada", example = "5")
    private Integer quantity;
    @Schema(description = "Precio por token", example = "12.75")
    private BigDecimal pricePerToken;
    @Schema(description = "Importe total", example = "63.75")
    private BigDecimal totalAmount;
    @Schema(description = "Fecha y hora de ejecución", example = "2026-06-09T12:30:00")
    private LocalDateTime timestamp;
}
