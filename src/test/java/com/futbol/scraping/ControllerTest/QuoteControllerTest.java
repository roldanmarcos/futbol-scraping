package com.futbol.scraping.ControllerTest;

import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.web.QuoteController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuoteService quoteService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ==================== Recalculate without strategy Tests ====================

    @Test
    void testRecalculate_WithoutStrategy() throws Exception {
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(100)
                .quotesGenerated(150)
                .status("SUCCESS")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.playersProcessed").value(100))
                .andExpect(jsonPath("$.quotesGenerated").value(150))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(quoteService, never()).setActiveStrategy(any());
        verify(quoteService, times(1)).recalculate();
    }

    // ==================== Recalculate with strategy Tests ====================

    @Test
    void testRecalculate_WithValidStrategy() throws Exception {
        String strategy = "PerformanceBasedStrategy";
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(100)
                .quotesGenerated(150)
                .status("SUCCESS")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate?strategy=" + strategy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(quoteService, times(1)).setActiveStrategy(strategy);
        verify(quoteService, times(1)).recalculate();
    }

    @Test
    void testRecalculate_WithBlankStrategy() throws Exception {
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(100)
                .quotesGenerated(150)
                .status("SUCCESS")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate?strategy=   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(quoteService, never()).setActiveStrategy(any());
        verify(quoteService, times(1)).recalculate();
    }

    @Test
    void testRecalculate_WithEmptyStrategy() throws Exception {
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(100)
                .quotesGenerated(150)
                .status("SUCCESS")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate?strategy="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(quoteService, never()).setActiveStrategy(any());
        verify(quoteService, times(1)).recalculate();
    }

    // ==================== Error handling Tests ====================

    @Test
    void testRecalculate_ServiceException() throws Exception {
        when(quoteService.recalculate())
                .thenThrow(new BusinessException("Error during recalculation"));

        mockMvc.perform(post("/quotes/recalculate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRecalculate_SetStrategyException() throws Exception {
        doThrow(new BusinessException("Invalid strategy"))
                .when(quoteService).setActiveStrategy("InvalidStrategy");

        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(0)
                .quotesGenerated(0)
                .status("FAILED")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate?strategy=InvalidStrategy"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Response validation Tests ====================

    @Test
    void testRecalculate_ResponseStructure() throws Exception {
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(150)
                .quotesGenerated(250)
                .status("SUCCESS")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.playersProcessed").exists())
                .andExpect(jsonPath("$.quotesGenerated").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.playersProcessed", greaterThan(0)))
                .andExpect(jsonPath("$.quotesGenerated", greaterThan(0)));
    }

    @Test
    void testRecalculate_ZeroProcessed() throws Exception {
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(0)
                .quotesGenerated(0)
                .status("NO_DATA")
                .build();

        when(quoteService.recalculate()).thenReturn(response);

        mockMvc.perform(post("/quotes/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersProcessed").value(0))
                .andExpect(jsonPath("$.quotesGenerated").value(0));
    }
}
