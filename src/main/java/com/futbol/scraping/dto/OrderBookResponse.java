package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Libro de ordenes de un jugador")
public class OrderBookResponse {
    @Schema(description = "Identificador del jugador", example = "100")
    private Long playerId;
    @Schema(description = "Nombre del jugador", example = "Lionel Messi")
    private String playerName;
    @Schema(description = "Precio actual del token (cotizacion del sistema)", example = "125.50")
    private BigDecimal price;
    @Schema(description = "Cantidad total de ordenes de compra pendientes", example = "15")
    private Integer buyQuantity;
    @Schema(description = "Cantidad de ordenes de compra pendientes", example = "3")
    private Integer buyOrderCount;
    @Schema(description = "Cantidad total de ordenes de venta pendientes", example = "10")
    private Integer sellQuantity;
    @Schema(description = "Cantidad de ordenes de venta pendientes", example = "2")
    private Integer sellOrderCount;
}
