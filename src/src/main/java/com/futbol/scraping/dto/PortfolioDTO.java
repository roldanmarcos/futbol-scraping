package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Schema(description = "Resumen de portafolio de un usuario")
public class PortfolioDTO {
    @Schema(example = "42")
    private Long userId;
    @Schema(example = "messi")
    private String username;
    @Schema(example = "1000.00")
    private BigDecimal totalInvested;
    @Schema(example = "1250.00")
    private BigDecimal currentValue;
    @Schema(example = "250.00")
    private BigDecimal profitLoss;
    @Schema(example = "25.00")
    private BigDecimal profitLossPercent;
    private List<PortfolioItemDTO> positions;
}
