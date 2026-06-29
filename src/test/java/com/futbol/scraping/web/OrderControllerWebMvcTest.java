package com.futbol.scraping.web;

import com.futbol.scraping.annotation.FutbolWebMvcIT;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderBookResponse;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.service.AuthorizationService;
import com.futbol.scraping.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FutbolWebMvcIT
@WebMvcTest(OrderController.class)
class OrderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private OrderService orderService;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void buy_ReturnsOk_WhenRequestIsValid() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(7L).buyerId(15L).quantity(3).build();

        OrderResponse response = OrderResponse.builder()
                .orderId(500L).playerId(7L).playerName("Julian Alvarez")
                .type("BUY").price(new BigDecimal("25.00")).quantity(3)
                .filledQuantity(3).remainingQuantity(0).status("FILLED")
                .totalAmount(new BigDecimal("75.00")).build();

        when(orderService.buy(any(BuyOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(500))
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.filledQuantity").value(3))
                .andExpect(jsonPath("$.totalAmount").value(75.00))
                .andExpect(jsonPath("$.status").value("FILLED"));

        verify(authorizationService).assertUserMatchesOrSuperuser(15L);

        ArgumentCaptor<BuyOrderRequest> captor = ArgumentCaptor.forClass(BuyOrderRequest.class);
        verify(orderService).buy(captor.capture());
        assertThat(captor.getValue().getPlayerId()).isEqualTo(7L);
        assertThat(captor.getValue().getBuyerId()).isEqualTo(15L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
    }

    @Test
    void sell_Returns403_WhenAuthorizationFails() throws Exception {
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(7L).sellerId(15L).quantity(2).build();

        doThrow(new AccessDeniedException("You can only access your own resources"))
                .when(authorizationService).assertUserMatchesOrSuperuser(15L);

        mockMvc.perform(post("/orders/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only access your own resources"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never()).sell(any());
    }

    @Test
    void buy_Returns400_WhenBusinessRuleFails() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(7L).buyerId(15L).quantity(0).build();

        when(orderService.buy(any(BuyOrderRequest.class)))
                .thenThrow(new BusinessException("Quantity must be positive"));

        mockMvc.perform(post("/orders/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Quantity must be positive"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void buy_Returns400_WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/orders/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\": 7, \"buyerId\": 15, \"quantity\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void sell_ReturnsOk_WhenRequestIsValid() throws Exception {
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(8L).sellerId(22L).quantity(2).build();

        OrderResponse response = OrderResponse.builder()
                .orderId(600L).playerId(8L).playerName("Lautaro Martinez")
                .type("SELL").price(new BigDecimal("40.00")).quantity(2)
                .filledQuantity(2).remainingQuantity(0).status("FILLED")
                .totalAmount(new BigDecimal("80.00")).build();

        when(orderService.sell(any(SellOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(600))
                .andExpect(jsonPath("$.type").value("SELL"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.filledQuantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(80.00))
                .andExpect(jsonPath("$.status").value("FILLED"));

        verify(authorizationService).assertUserMatchesOrSuperuser(22L);

        ArgumentCaptor<SellOrderRequest> captor = ArgumentCaptor.forClass(SellOrderRequest.class);
        verify(orderService).sell(captor.capture());
        assertThat(captor.getValue().getPlayerId()).isEqualTo(8L);
        assertThat(captor.getValue().getSellerId()).isEqualTo(22L);
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void cancel_ReturnsOk_WhenOwnOrder() throws Exception {
        mockMvc.perform(post("/orders/100/cancel")
                        .param("userId", "15")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancelled"));

        verify(authorizationService).assertUserMatchesOrSuperuser(15L);
        verify(orderService).cancelOrder(100L, 15L);
    }

    @Test
    void book_ReturnsOk_WhenPlayerExists() throws Exception {
        OrderBookResponse response = OrderBookResponse.builder()
                .playerId(1L).playerName("Messi")
                .price(new BigDecimal("120.50"))
                .buyQuantity(15).buyOrderCount(3)
                .sellQuantity(10).sellOrderCount(2)
                .build();

        when(orderService.getOrderBook(1L)).thenReturn(response);

        mockMvc.perform(get("/orders/book/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(1))
                .andExpect(jsonPath("$.playerName").value("Messi"))
                .andExpect(jsonPath("$.price").value(120.50))
                .andExpect(jsonPath("$.buyQuantity").value(15))
                .andExpect(jsonPath("$.sellQuantity").value(10));
    }
}
