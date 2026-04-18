package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlayerRankingDTO {
    private Integer rank;
    private Long playerId;
    private String playerName;
    private String league;
    private String team;
    private String position;
    private BigDecimal currentQuote;
    private BigDecimal score;
    private String strategyVersion;
}
