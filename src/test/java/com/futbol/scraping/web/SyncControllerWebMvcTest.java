package com.futbol.scraping.web;

import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.GlobalExceptionHandler;
import com.futbol.scraping.model.User;
import com.futbol.scraping.service.ScrapingService;
import com.futbol.scraping.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SyncController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SyncControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private ScrapingService scrapingService;

    @MockBean
    private UserService userService;

    @Test
    void syncPlayers_WithLeague_ReturnsOk() throws Exception {
        when(scrapingService.syncLeague("Premier League")).thenReturn(12);

        mockMvc.perform(post("/sync/players").param("league", "Premier League"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersSync").value(12))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(scrapingService).syncLeague("Premier League");
    }

}
