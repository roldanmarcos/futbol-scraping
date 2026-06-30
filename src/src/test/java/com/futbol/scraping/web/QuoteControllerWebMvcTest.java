package com.futbol.scraping.web;

import com.futbol.scraping.annotation.FutbolWebMvcIT;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@FutbolWebMvcIT
@WebMvcTest(QuoteController.class)
class QuoteControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private QuoteService quoteService;

    @Test
    void recalculate_WithStrategy_ReturnsOk() throws Exception {
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(20)
                .quotesGenerated(20)
                .strategyUsed("positionWeighted")
                .calculatedAt(LocalDateTime.of(2024, 10, 21, 11, 0))
                .status("SUCCESS")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate").param("strategy", "positionWeighted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersProcessed").value(20))
                .andExpect(jsonPath("$.quotesGenerated").value(20))
                .andExpect(jsonPath("$.strategyUsed").value("positionWeighted"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(quoteService).setActiveStrategy("positionWeighted");
        verify(quoteService).recalculate();
    }

    @Test
    void recalculate_WhenStrategyIsUnknown_Returns400() throws Exception {
        doThrow(new IllegalArgumentException("Unknown strategy: random"))
                .when(quoteService).setActiveStrategy("random");

        mockMvc.perform(post("/quotes/recalculate").param("strategy", "random"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request parameter or format"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(quoteService, never()).recalculate();
    }
}
