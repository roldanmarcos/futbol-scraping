package com.futbol.scraping.web;

import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.service.AuthorizationService;
import com.futbol.scraping.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Compra y venta de tokens de jugadores")
public class OrderController {

    private final OrderService orderService;
    private final AuthorizationService authorizationService;

    @PostMapping("/buy")
    @Operation(summary = "Comprar tokens", description = "Registra una orden de compra de tokens para un jugador.")
    public ResponseEntity<OrderResponse> buy(
            @RequestBody(description = "Datos de la orden de compra") @org.springframework.web.bind.annotation.RequestBody BuyOrderRequest request) {
        log.info("POST /orders/buy");
        authorizationService.assertUserMatchesOrSuperuser(request.getBuyerId());
        return ResponseEntity.ok(orderService.buy(request));
    }

    @PostMapping("/sell")
    @Operation(summary = "Vender tokens", description = "Registra una orden de venta de tokens para un jugador.")
    public ResponseEntity<OrderResponse> sell(
            @RequestBody(description = "Datos de la orden de venta") @org.springframework.web.bind.annotation.RequestBody SellOrderRequest request) {
        log.info("POST /orders/sell");
        authorizationService.assertUserMatchesOrSuperuser(request.getSellerId());
        return ResponseEntity.ok(orderService.sell(request));
    }
}
