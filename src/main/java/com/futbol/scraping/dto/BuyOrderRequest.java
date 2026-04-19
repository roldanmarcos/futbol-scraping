package com.futbol.scraping.dto;

import lombok.Data;

@Data
public class BuyOrderRequest {
    private Long playerId;
    private Long buyerId;
    private Integer quantity;
}
