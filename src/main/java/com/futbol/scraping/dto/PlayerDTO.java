package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlayerDTO {
    private Long id;
    private String name;
    private String league;
    private String team;
    private String position;
    private String nationality;
    private Integer age;
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private String whoscoredId;
    private BigDecimal currentQuote;
    private LocalDateTime lastQuoteDate;
}
