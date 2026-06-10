package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Detalle de un jugador")
public class PlayerDetailDTO {
    @Schema(example = "100")
    private Long id;
    @Schema(example = "Lionel Messi")
    private String name;
    @Schema(example = "La Liga")
    private String league;
    @Schema(example = "Inter Miami")
    private String team;
    @Schema(example = "Forward")
    private String position;
    @Schema(example = "37")
    private Integer age;
    @Schema(example = "72")
    private Integer weight;
    @Schema(example = "28")
    private Integer appearances;
    @Schema(example = "20")
    private Integer goals;
    @Schema(example = "12")
    private Integer assists;
    @Schema(example = "whoscored-100")
    private String whoscoredId;
    @Schema(example = "https://www.whoscored.com/Players/100")
    private String url;
    @Schema(example = "12.75")
    private BigDecimal currentQuote;
    @Schema(example = "2026-06-09T12:30:00")
    private LocalDateTime lastQuoteDate;
    private List<QuoteDTO> recentQuotes;
}
