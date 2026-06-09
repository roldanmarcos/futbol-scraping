package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Solicitud para comprar tokens de un jugador")
public class BuyOrderRequest {
    @Schema(description = "Identificador del jugador", example = "100")
    private Long playerId;
    @Schema(description = "Identificador del comprador", example = "42")
    private Long buyerId;
    @Schema(description = "Cantidad de tokens a comprar", example = "5")
    private Integer quantity;
}
