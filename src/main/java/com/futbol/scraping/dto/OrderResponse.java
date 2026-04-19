package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private Long transactionId;
    private Long playerId;
    private String playerName;
    private String type;
    private Integer quantity;
    private BigDecimal pricePerToken;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;
}
