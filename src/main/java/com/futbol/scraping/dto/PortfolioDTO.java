package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PortfolioDTO {
    private Long userId;
    private String username;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercent;
    private List<PortfolioItemDTO> positions;
}
