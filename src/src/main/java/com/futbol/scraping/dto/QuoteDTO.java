package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Cotización de un jugador")
public class QuoteDTO {
    @Schema(example = "5001")
    private Long id;
    @Schema(example = "100")
    private Long playerId;
    @Schema(example = "Lionel Messi")
    private String playerName;
    @Schema(example = "12.75")
    private BigDecimal value;
    @Schema(example = "2026-06-09T12:30:00")
    private LocalDateTime quoteDate;
    @Schema(example = "v1")
    private String strategyVersion;
    @Schema(example = "98.4")
    private BigDecimal baseScore;
}
