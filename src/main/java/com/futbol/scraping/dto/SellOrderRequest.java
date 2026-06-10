package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Solicitud para vender tokens de un jugador")
public class SellOrderRequest {
    @Schema(description = "Identificador del jugador", example = "100")
    private Long playerId;
    @Schema(description = "Identificador del vendedor", example = "42")
    private Long sellerId;
    @Schema(description = "Cantidad de tokens a vender", example = "3")
    private Integer quantity;
}
