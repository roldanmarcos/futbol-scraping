package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Resultado del recálculo de cotizaciones")
public class RecalculateResponse {
    @Schema(description = "Cantidad de jugadores procesados", example = "2757")
    private int playersProcessed;
    @Schema(description = "Cantidad de cotizaciones generadas", example = "2757")
    private int quotesGenerated;
    @Schema(description = "Estrategia utilizada", example = "default")
    private String strategyUsed;
    @Schema(description = "Marca temporal del cálculo", example = "2026-06-09T12:30:00")
    private LocalDateTime calculatedAt;
    @Schema(description = "Estado de la operación", example = "SUCCESS")
    private String status;
}
