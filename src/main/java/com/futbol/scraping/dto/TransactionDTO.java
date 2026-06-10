package com.futbol.scraping.dto;

import com.futbol.scraping.model.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Transacción realizada por un usuario")
public class TransactionDTO {
    @Schema(example = "7001")
    private Long id;
    @Schema(example = "100")
    private Long playerId;
    @Schema(example = "Lionel Messi")
    private String playerName;
    @Schema(example = "BUY")
    private Transaction.TransactionType type;
    @Schema(example = "5")
    private Integer quantity;
    @Schema(example = "12.75")
    private BigDecimal pricePerToken;
    @Schema(example = "63.75")
    private BigDecimal totalAmount;
    @Schema(example = "2026-06-09T12:30:00")
    private LocalDateTime createdAt;
}
