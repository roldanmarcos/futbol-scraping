package com.futbol.scraping.dto;

import lombok.Data;

@Data
public class SellOrderRequest {
    private Long playerId;
    private Long sellerId;
    private Integer quantity;
}
