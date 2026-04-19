package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecalculateResponse {
    private int playersProcessed;
    private int quotesGenerated;
    private String strategyUsed;
    private LocalDateTime calculatedAt;
    private String status;
}
