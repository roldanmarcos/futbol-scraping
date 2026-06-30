package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Elemento del ranking de jugadores")
public class PlayerRankingDTO {
    @Schema(example = "1")
    private Integer rank;
    @Schema(example = "100")
    private Long playerId;
    @Schema(example = "Lionel Messi")
    private String playerName;
    @Schema(example = "La Liga")
    private String league;
    @Schema(example = "Inter Miami")
    private String team;
    @Schema(example = "Forward")
    private String position;
    @Schema(example = "12.75")
    private BigDecimal currentQuote;
    @Schema(example = "98.4")
    private BigDecimal score;
    @Schema(example = "v1")
    private String strategyVersion;
}
