package com.futbol.scraping.web;

import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.GlobalExceptionHandler;
import com.futbol.scraping.service.AuthorizationService;
import com.futbol.scraping.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
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
                .playerId(7L)
                .buyerId(15L)
                .quantity(3)
                .build();

        OrderResponse response = OrderResponse.builder()
                .transactionId(101L)
                .playerId(7L)
                .playerName("Julian Alvarez")
                .type("BUY")
                .quantity(3)
                .pricePerToken(new BigDecimal("25.00"))
                .totalAmount(new BigDecimal("75.00"))
                .timestamp(LocalDateTime.of(2024, 10, 20, 10, 15, 0))
                .build();

        when(orderService.buy(any(BuyOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(101))
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.totalAmount").value(75.00));

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
                .playerId(7L)
                .sellerId(15L)
                .quantity(2)
                .build();

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
                .playerId(7L)
                .buyerId(15L)
                .quantity(0)
                .build();

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
}
