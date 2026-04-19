package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PlayerDetailDTO {
    private Long id;
    private String name;
    private String league;
    private String team;
    private String position;
    private Integer age;
    private Integer weight;
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private String whoscoredId;
    private String url;
    private BigDecimal currentQuote;
    private LocalDateTime lastQuoteDate;
    private List<QuoteDTO> recentQuotes;
}
