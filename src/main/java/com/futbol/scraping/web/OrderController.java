package com.futbol.scraping.web;

import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public ResponseEntity<OrderResponse> buy(@RequestBody BuyOrderRequest request) {
        log.info("POST /orders/buy");
        
        if (request.getBuyerId() == null) {
            throw new IllegalArgumentException("buyerId is required");
        }
        if (request.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        
        return ResponseEntity.ok(orderService.buy(request));
    }

    @PostMapping("/sell")
    public ResponseEntity<OrderResponse> sell(@RequestBody SellOrderRequest request) {
        log.info("POST /orders/sell");
        
        if (request.getSellerId() == null) {
            throw new IllegalArgumentException("sellerId is required");
        }
        if (request.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        
        return ResponseEntity.ok(orderService.sell(request));
    }
}
