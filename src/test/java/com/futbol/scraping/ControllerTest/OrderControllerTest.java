package com.futbol.scraping.ControllerTest;

import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.scraping.web.OrderController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Buy Order Tests ====================

    @Test
    void testBuy_Success() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .buyerId(1L)
                .playerId(100L)
                .quantity(10)
                .build();

        OrderResponse response = OrderResponse.builder()
                .transactionId(1L)
                .playerId(100L)
                .playerName("Lionel Messi")
                .type("BUY")
                .quantity(10)
                .pricePerToken(new BigDecimal("105.00"))
                .totalAmount(new BigDecimal("1050.00"))
                .timestamp(java.time.LocalDateTime.now())
                .build();

        when(orderService.buy(any(BuyOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.transactionId").value(1L))
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.totalAmount").value(1050.0));
    }

    @Test
    void testBuy_SingleShare() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .buyerId(1L)
                .playerId(100L)
                .quantity(1)
                .build();

        OrderResponse response = OrderResponse.builder()
                .transactionId(1L)
                .playerId(100L)
                .playerName("Lionel Messi")
                .type("BUY")
                .quantity(1)
                .pricePerToken(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("100.00"))
                .timestamp(java.time.LocalDateTime.now())
                .build();

        when(orderService.buy(any(BuyOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1));
    }

    @Test
    void testBuy_InsufficientBalance() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .buyerId(1L)
                .playerId(100L)
                .quantity(1000)
                .build();

        when(orderService.buy(any(BuyOrderRequest.class)))
                .thenThrow(new BusinessException("Insufficient balance"));

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBuy_PlayerNotFound() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .buyerId(1L)
                .playerId(999L)
                .quantity(10)
                .build();

        when(orderService.buy(any(BuyOrderRequest.class)))
                .thenThrow(new BusinessException("Player not found"));

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBuy_InvalidQuantity() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .buyerId(1L)
                .playerId(100L)
                .quantity(0)
                .build();

        when(orderService.buy(any(BuyOrderRequest.class)))
                .thenThrow(new BusinessException("Quantity must be positive"));

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBuy_MissingUserId() throws Exception {
        String json = "{\"playerId\": 100, \"quantity\": 10}";

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBuy_MissingPlayerId() throws Exception {
        String json = "{\"buyerId\": 1, \"quantity\": 10}";

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBuy_MissingQuantity() throws Exception {
        String json = "{\"buyerId\": 1, \"playerId\": 100}";

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBuy_NegativeQuantity() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .buyerId(1L)
                .playerId(100L)
                .quantity(-5)
                .build();

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBuy_ZeroQuantity() throws Exception {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .buyerId(1L)
                .playerId(100L)
                .quantity(0)
                .build();

        mockMvc.perform(post("/orders/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== Sell Order Tests ====================

    @Test
    void testSell_Success() throws Exception {
        SellOrderRequest request = SellOrderRequest.builder()
                .sellerId(1L)
                .playerId(100L)
                .quantity(5)
                .build();

        OrderResponse response = OrderResponse.builder()
                .transactionId(2L)
                .playerId(100L)
                .playerName("Lionel Messi")
                .type("SELL")
                .quantity(5)
                .pricePerToken(new BigDecimal("105.00"))
                .totalAmount(new BigDecimal("525.00"))
                .timestamp(java.time.LocalDateTime.now())
                .build();

        when(orderService.sell(any(SellOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.transactionId").value(2L))
                .andExpect(jsonPath("$.type").value("SELL"))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void testSell_InsufficientShares() throws Exception {
        SellOrderRequest request = SellOrderRequest.builder()
                .sellerId(1L)
                .playerId(100L)
                .quantity(1000)
                .build();

        when(orderService.sell(any(SellOrderRequest.class)))
                .thenThrow(new BusinessException("Insufficient shares"));

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSell_InvalidQuantity() throws Exception {
        SellOrderRequest request = SellOrderRequest.builder()
                .sellerId(1L)
                .playerId(100L)
                .quantity(-5)
                .build();

        when(orderService.sell(any(SellOrderRequest.class)))
                .thenThrow(new BusinessException("Quantity must be positive"));

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSell_MissingFields() throws Exception {
        String json = "{\"userId\": 1}";

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSell_MissingSellerId() throws Exception {
        String json = "{\"playerId\": 100, \"quantity\": 5}";

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSell_MissingPlayerId() throws Exception {
        String json = "{\"sellerId\": 1, \"quantity\": 5}";

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSell_MissingQuantity() throws Exception {
        String json = "{\"sellerId\": 1, \"playerId\": 100}";

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSell_ZeroQuantity() throws Exception {
        SellOrderRequest request = SellOrderRequest.builder()
                .sellerId(1L)
                .playerId(100L)
                .quantity(0)
                .build();

        mockMvc.perform(post("/orders/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
