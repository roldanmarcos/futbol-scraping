package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class QuoteDTO {
    private Long id;
    private Long playerId;
    private String playerName;
    private BigDecimal value;
    private LocalDateTime quoteDate;
    private String strategyVersion;
    private BigDecimal baseScore;
}
