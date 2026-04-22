package com.futbol.scraping.ControllerTest;

import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.PlayerDetailDTO;
import com.futbol.scraping.dto.PlayerRankingDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.web.PlayerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlayerService playerService;

    @MockBean
    private QuoteService quoteService;

    // ==================== getPlayers Tests ====================

    @Test
    void testGetPlayers_NoFilters() throws Exception {
        PlayerDTO player1 = PlayerDTO.builder()
                .id(1L).name("Lionel Messi").league("La Liga").team("Barcelona").position("Forward").build();
        PlayerDTO player2 = PlayerDTO.builder()
                .id(2L).name("Cristiano Ronaldo").league("Serie A").team("Juventus").position("Forward").build();

        when(playerService.getPlayers(null, null, null))
                .thenReturn(List.of(player1, player2));

        mockMvc.perform(get("/players"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Lionel Messi"))
                .andExpect(jsonPath("$[1].name").value("Cristiano Ronaldo"));
    }

    @Test
    void testGetPlayers_WithLeagueFilter() throws Exception {
        PlayerDTO player = PlayerDTO.builder()
                .id(1L).name("Lionel Messi").league("La Liga").team("Barcelona").position("Forward").build();

        when(playerService.getPlayers("La Liga", null, null))
                .thenReturn(List.of(player));

        mockMvc.perform(get("/players?league=La Liga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].league").value("La Liga"));
    }

    @Test
    void testGetPlayers_WithTeamFilter() throws Exception {
        PlayerDTO player = PlayerDTO.builder()
                .id(1L).name("Lionel Messi").league("La Liga").team("Barcelona").position("Forward").build();

        when(playerService.getPlayers(null, "Barcelona", null))
                .thenReturn(List.of(player));

        mockMvc.perform(get("/players?team=Barcelona"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].team").value("Barcelona"));
    }

    @Test
    void testGetPlayers_WithPositionFilter() throws Exception {
        PlayerDTO player = PlayerDTO.builder()
                .id(1L).name("Lionel Messi").league("La Liga").team("Barcelona").position("Forward").build();

        when(playerService.getPlayers(null, null, "Forward"))
                .thenReturn(List.of(player));

        mockMvc.perform(get("/players?position=Forward"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value("Forward"));
    }

    @Test
    void testGetPlayers_AllFilters() throws Exception {
        PlayerDTO player = PlayerDTO.builder()
                .id(1L).name("Lionel Messi").league("La Liga").team("Barcelona").position("Forward").build();

        when(playerService.getPlayers("La Liga", "Barcelona", "Forward"))
                .thenReturn(List.of(player));

        mockMvc.perform(get("/players?league=La Liga&team=Barcelona&position=Forward"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testGetPlayers_EmptyResult() throws Exception {
        when(playerService.getPlayers("NonExistent", null, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/players?league=NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== getRanking Tests ====================

    @Test
    void testGetRanking_Success() throws Exception {
        PlayerRankingDTO rank1 = PlayerRankingDTO.builder()
                .rank(1).playerId(1L).playerName("Lionel Messi").score(new BigDecimal("95.5")).build();
        PlayerRankingDTO rank2 = PlayerRankingDTO.builder()
                .rank(2).playerId(2L).playerName("Cristiano Ronaldo").score(new BigDecimal("94.0")).build();

        when(quoteService.getRanking()).thenReturn(List.of(rank1, rank2));

        mockMvc.perform(get("/players/ranking"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[1].rank").value(2));
    }

    @Test
    void testGetRanking_EmptyList() throws Exception {
        when(quoteService.getRanking()).thenReturn(List.of());

        mockMvc.perform(get("/players/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== getPlayer Tests ====================

    @Test
    void testGetPlayer_Success() throws Exception {
        Long playerId = 1L;
        PlayerDetailDTO player = PlayerDetailDTO.builder()
                .id(playerId)
                .name("Lionel Messi")
                .league("La Liga")
                .team("Barcelona")
                .position("Forward")
                .age(34)
                .goals(25)
                .assists(10)
                .build();

        when(playerService.getPlayerById(playerId)).thenReturn(player);

        mockMvc.perform(get("/players/{id}", playerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(playerId))
                .andExpect(jsonPath("$.name").value("Lionel Messi"))
                .andExpect(jsonPath("$.age").value(34))
                .andExpect(jsonPath("$.goals").value(25));
    }

    @Test
    void testGetPlayer_NotFound() throws Exception {
        Long playerId = 999L;
        when(playerService.getPlayerById(playerId))
                .thenThrow(new ResourceNotFoundException("Player not found"));

        mockMvc.perform(get("/players/{id}", playerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPlayer_InvalidId() throws Exception {
        mockMvc.perform(get("/players/{id}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    // ==================== getPlayerQuotes Tests ====================

    @Test
    void testGetPlayerQuotes_WithoutDate() throws Exception {
        Long playerId = 1L;
        QuoteDTO quote1 = QuoteDTO.builder()
                .id(1L).playerId(playerId).playerName("Messi")
                .value(new BigDecimal("100.50")).quoteDate(LocalDateTime.now())
                .strategyVersion("v1").baseScore(new BigDecimal("85.0")).build();
        QuoteDTO quote2 = QuoteDTO.builder()
                .id(2L).playerId(playerId).playerName("Messi")
                .value(new BigDecimal("101.00")).quoteDate(LocalDateTime.now())
                .strategyVersion("v1").baseScore(new BigDecimal("85.5")).build();

        when(quoteService.getPlayerQuotes(playerId))
                .thenReturn(List.of(quote1, quote2));

        mockMvc.perform(get("/players/{id}/quotes", playerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].value").value(100.50))
                .andExpect(jsonPath("$[1].value").value(101.00));
    }

    @Test
    void testGetPlayerQuotes_WithDate() throws Exception {
        Long playerId = 1L;
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 10, 30);
        QuoteDTO quote = QuoteDTO.builder()
                .id(1L).playerId(playerId).playerName("Messi")
                .value(new BigDecimal("100.50")).quoteDate(date)
                .strategyVersion("v1").baseScore(new BigDecimal("85.0")).build();

        when(quoteService.getQuoteAtDate(playerId, date))
                .thenReturn(quote);

        mockMvc.perform(get("/players/{id}/quotes?date=2024-01-15T10:30:00", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].value").value(100.50));
    }

    @Test
    void testGetPlayerQuotes_NoQuotesFound() throws Exception {
        Long playerId = 1L;
        when(quoteService.getPlayerQuotes(playerId))
                .thenReturn(List.of());

        mockMvc.perform(get("/players/{id}/quotes", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetPlayerQuotes_PlayerNotFound() throws Exception {
        Long playerId = 999L;
        when(quoteService.getPlayerQuotes(playerId))
                .thenThrow(new ResourceNotFoundException("Player not found"));

        mockMvc.perform(get("/players/{id}/quotes", playerId))
                .andExpect(status().isNotFound());
    }
}
