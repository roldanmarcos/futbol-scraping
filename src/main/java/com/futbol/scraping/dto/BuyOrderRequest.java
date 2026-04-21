package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BuyOrderRequest {
    private Long playerId;
    private Long buyerId;
    private Integer quantity;
}
