package com.futbol.scraping.web;

import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.exception.GlobalExceptionHandler;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlayerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
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
        LocalDate inputDate = LocalDate.of(2024, 10, 2);
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
    void getPlayerQuotes_WithInvalidDate_Returns400() throws Exception {
        mockMvc.perform(get("/players/1/quotes").param("date", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request parameter or format"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
