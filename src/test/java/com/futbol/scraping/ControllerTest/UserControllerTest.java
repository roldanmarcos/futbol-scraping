package com.futbol.scraping.ControllerTest;

import com.futbol.scraping.dto.PortfolioDTO;
import com.futbol.scraping.dto.TransactionDTO;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.service.AuthorizationService;
import com.futbol.scraping.service.UserService;
import com.futbol.scraping.web.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthorizationService authorizationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ==================== Portfolio Tests ====================

    @Test
    void testGetPortfolio_Success() throws Exception {
        Long userId = 1L;
        PortfolioDTO portfolio = PortfolioDTO.builder()
                .userId(userId)
                .username("john_doe")
                .totalInvested(new BigDecimal("5000.00"))
                .currentValue(new BigDecimal("15000.00"))
                .profitLoss(new BigDecimal("10000.00"))
                .profitLossPercent(new BigDecimal("200.00"))
                .positions(List.of())
                .build();

        when(userService.getPortfolio(userId)).thenReturn(portfolio);

        mockMvc.perform(get("/users/{id}/portfolio", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.currentValue").value(15000.0))
                .andExpect(jsonPath("$.totalInvested").value(5000.0));
    }

    @Test
    void testGetPortfolio_UserNotFound() throws Exception {
        Long userId = 999L;
        when(userService.getPortfolio(userId))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/{id}/portfolio", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPortfolio_InvalidUserId() throws Exception {
        mockMvc.perform(get("/users/{id}/portfolio", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetPortfolio_ZeroBalance() throws Exception {
        Long userId = 2L;
        PortfolioDTO portfolio = PortfolioDTO.builder()
                .userId(userId)
                .username("empty_user")
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .profitLoss(BigDecimal.ZERO)
                .profitLossPercent(BigDecimal.ZERO)
                .positions(List.of())
                .build();

        when(userService.getPortfolio(userId)).thenReturn(portfolio);

        mockMvc.perform(get("/users/{id}/portfolio", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(0.0));
    }

    // ==================== Transactions Tests ====================

    @Test
    void testGetTransactions_Success() throws Exception {
        Long userId = 1L;
        TransactionDTO transaction = TransactionDTO.builder()
                .id(1L)
                .playerId(100L)
                .playerName("Lionel Messi")
                .type(com.futbol.scraping.model.Transaction.TransactionType.BUY)
                .quantity(10)
                .pricePerToken(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("500.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getTransactions(userId))
                .thenReturn(List.of(transaction));

        mockMvc.perform(get("/users/{id}/transactions", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("BUY"))
                .andExpect(jsonPath("$[0].totalAmount").value(500.0));
    }

    @Test
    void testGetTransactions_EmptyList() throws Exception {
        Long userId = 1L;
        when(userService.getTransactions(userId)).thenReturn(List.of());

        mockMvc.perform(get("/users/{id}/transactions", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetTransactions_MultipleTransactions() throws Exception {
        Long userId = 1L;
        TransactionDTO tx1 = TransactionDTO.builder()
                .id(1L).playerId(100L).playerName("Messi")
                .type(com.futbol.scraping.model.Transaction.TransactionType.BUY)
                .quantity(10).pricePerToken(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("500.00"))
                .createdAt(LocalDateTime.now()).build();
        TransactionDTO tx2 = TransactionDTO.builder()
                .id(2L).playerId(100L).playerName("Messi")
                .type(com.futbol.scraping.model.Transaction.TransactionType.SELL)
                .quantity(5).pricePerToken(new BigDecimal("60.00"))
                .totalAmount(new BigDecimal("300.00"))
                .createdAt(LocalDateTime.now()).build();

        when(userService.getTransactions(userId))
                .thenReturn(List.of(tx1, tx2));

        mockMvc.perform(get("/users/{id}/transactions", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("BUY"))
                .andExpect(jsonPath("$[1].type").value("SELL"));
    }

    @Test
    void testGetTransactions_UserNotFound() throws Exception {
        Long userId = 999L;
        when(userService.getTransactions(userId))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/users/{id}/transactions", userId))
                .andExpect(status().isNotFound());
    }
}
