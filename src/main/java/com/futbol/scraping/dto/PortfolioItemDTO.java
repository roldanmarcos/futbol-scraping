package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Posición individual dentro de un portafolio")
public class PortfolioItemDTO {
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
    @Schema(example = "10")
    private Integer quantity;
    @Schema(example = "100.00")
    private BigDecimal avgBuyPrice;
    @Schema(example = "125.00")
    private BigDecimal currentPrice;
    @Schema(example = "1000.00")
    private BigDecimal totalInvested;
    @Schema(example = "1250.00")
    private BigDecimal currentValue;
    @Schema(example = "250.00")
    private BigDecimal profitLoss;
    @Schema(example = "25.00")
    private BigDecimal profitLossPercent;
}
