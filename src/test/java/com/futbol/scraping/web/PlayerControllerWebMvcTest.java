package com.futbol.scraping.web;

import com.futbol.scraping.annotation.FutbolWebMvcIT;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.PlayerRankingDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FutbolWebMvcIT
@WebMvcTest(PlayerController.class)
class PlayerControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private PlayerService playerService;

    @MockBean
    private QuoteService quoteService;

    @Test
    void getPlayers_ReturnsOk() throws Exception {
        PlayerDTO player = PlayerDTO.builder()
                .id(1L)
                .name("Lionel Messi")
                .league("Ligue 1")
                .team("PSG")
                .position("FW")
                .currentQuote(new BigDecimal("150.00"))
                .build();

        when(playerService.getPlayers("Ligue 1", "PSG", "FW")).thenReturn(List.of(player));

        mockMvc.perform(get("/players")
                        .param("league", "Ligue 1")
                        .param("team", "PSG")
                        .param("position", "FW")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Lionel Messi"))
                .andExpect(jsonPath("$[0].currentQuote").value(150.00));

        verify(playerService).getPlayers("Ligue 1", "PSG", "FW");
    }

    @Test
    void getPlayer_WhenNotFound_Returns404() throws Exception {
        when(playerService.getPlayerById(99L)).thenThrow(new ResourceNotFoundException("Player not found"));

        mockMvc.perform(get("/players/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Player not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getPlayerQuotes_WithDate_ReturnsSingleQuote() throws Exception {
        // Arrange - LocalDate que envía el cliente
        LocalDate inputDate = LocalDate.of(2024, Month.OCTOBER, 2);
        // LocalDateTime convertido por el controlador: atTime(23, 59, 59)
        LocalDateTime expectedDateTime = inputDate.atTime(23, 59, 59);

        QuoteDTO quote = QuoteDTO.builder()
                .id(10L)
                .playerId(1L)
                .playerName("Lionel Messi")
                .value(new BigDecimal("180.50"))
                .quoteDate(expectedDateTime)
                .build();

        when(quoteService.getQuoteAtDate(1L, expectedDateTime)).thenReturn(quote);

        // Act
        mockMvc.perform(get("/players/1/quotes").param("date", "2024-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].playerId").value(1))
                .andExpect(jsonPath("$[0].value").value(180.50));

        // Assert - Verifica que se llamó con el DateTime convertido correcto
        verify(quoteService).getQuoteAtDate(1L, expectedDateTime);
    }

    @Test
    void getRanking_ReturnsOk() throws Exception {
        PlayerRankingDTO rankingEntry = PlayerRankingDTO.builder()
                .rank(1)
                .playerId(1L)
                .playerName("Lionel Messi")
                .league("MLS")
                .team("Inter Miami")
                .position("FW")
                .currentQuote(new BigDecimal("150.00"))
                .score(new BigDecimal("98.4"))
                .strategyVersion("v1")
                .build();

        when(quoteService.getRanking()).thenReturn(List.of(rankingEntry));

        mockMvc.perform(get("/players/ranking").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].playerName").value("Lionel Messi"))
                .andExpect(jsonPath("$[0].score").value(98.4));

        verify(quoteService).getRanking();
    }

    @Test
    void getPlayerQuotes_WithoutDate_ReturnsFullHistory() throws Exception {
        QuoteDTO q1 = QuoteDTO.builder().id(1L).playerId(1L).playerName("Lionel Messi").value(new BigDecimal("100.00")).build();
        QuoteDTO q2 = QuoteDTO.builder().id(2L).playerId(1L).playerName("Lionel Messi").value(new BigDecimal("120.00")).build();

        when(quoteService.getPlayerQuotes(1L)).thenReturn(List.of(q1, q2));

        mockMvc.perform(get("/players/1/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].value").value(100.00))
                .andExpect(jsonPath("$[1].value").value(120.00));

        verify(quoteService).getPlayerQuotes(1L);
    }

    @Test
    void getPlayerQuotes_WithInvalidDate_Returns400() throws Exception {
        mockMvc.perform(get("/players/1/quotes").param("date", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request parameter or format"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
