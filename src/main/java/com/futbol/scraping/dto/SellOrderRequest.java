package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SellOrderRequest {
    private Long playerId;
    private Long sellerId;
    private Integer quantity;
}
