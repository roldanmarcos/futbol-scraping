package com.futbol.scraping.web;

import com.futbol.scraping.annotation.FutbolWebMvcIT;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.dto.PortfolioDTO;
import com.futbol.scraping.dto.PortfolioItemDTO;
import com.futbol.scraping.dto.TransactionDTO;
import com.futbol.scraping.model.Transaction;
import com.futbol.scraping.service.AuthorizationService;
import com.futbol.scraping.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FutbolWebMvcIT
@WebMvcTest(UserController.class)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void getPortfolio_ReturnsOk() throws Exception {
        PortfolioItemDTO item = PortfolioItemDTO.builder()
                .playerId(10L)
                .playerName("Lautaro Martinez")
                .quantity(2)
                .avgBuyPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("120.00"))
                .build();

        PortfolioDTO portfolio = PortfolioDTO.builder()
                .userId(1L)
                .username("ana")
                .totalInvested(new BigDecimal("200.00"))
                .currentValue(new BigDecimal("240.00"))
                .profitLoss(new BigDecimal("40.00"))
                .profitLossPercent(new BigDecimal("20.00"))
                .positions(List.of(item))
                .build();

        when(userService.getPortfolio(1L)).thenReturn(portfolio);

        mockMvc.perform(get("/users/1/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.positions[0].playerId").value(10))
                .andExpect(jsonPath("$.positions[0].playerName").value("Lautaro Martinez"));

        verify(authorizationService).assertUserMatchesOrSuperuser(1L);
        verify(userService).getPortfolio(1L);
    }

    @Test
    void getTransactions_ReturnsOk() throws Exception {
        TransactionDTO tx = TransactionDTO.builder()
                .id(70L)
                .playerId(10L)
                .playerName("Lautaro Martinez")
                .type(Transaction.TransactionType.BUY)
                .quantity(2)
                .pricePerToken(new BigDecimal("120.00"))
                .totalAmount(new BigDecimal("240.00"))
                .createdAt(LocalDateTime.of(2024, 10, 20, 18, 30))
                .build();

        when(userService.getTransactions(1L)).thenReturn(List.of(tx));

        mockMvc.perform(get("/users/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(70))
                .andExpect(jsonPath("$[0].type").value("BUY"))
                .andExpect(jsonPath("$[0].totalAmount").value(240.00));

        verify(authorizationService).assertUserMatchesOrSuperuser(1L);
        verify(userService).getTransactions(1L);
    }

    @Test
    void getPortfolio_WhenAuthorizationFails_Returns403() throws Exception {
        doThrow(new AccessDeniedException("You can only access your own resources"))
                .when(authorizationService).assertUserMatchesOrSuperuser(99L);

        mockMvc.perform(get("/users/99/portfolio"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only access your own resources"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

