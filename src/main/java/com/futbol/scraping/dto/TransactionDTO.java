package com.futbol.scraping.dto;

import com.futbol.scraping.model.Transaction;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDTO {
    private Long id;
    private Long playerId;
    private String playerName;
    private Transaction.TransactionType type;
    private Integer quantity;
    private BigDecimal pricePerToken;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
