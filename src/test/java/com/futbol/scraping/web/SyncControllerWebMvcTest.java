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

    @Test
    void createUser_WhenBusinessRuleFails_Returns400() throws Exception {
        when(userService.createUser(eq("pepe"), eq("pepe@mail.com"), any(BigDecimal.class)))
                .thenThrow(new BusinessException("Username already exists: pepe"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pepe\",\"email\":\"pepe@mail.com\",\"balance\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists: pepe"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createUser_WhenJsonIsMalformed_Returns400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pepe\",\"email\":\"pepe@mail.com\",\"balance\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createUser_ReturnsCreatedUserPayload() throws Exception {
        User user = User.builder()
                .id(5L)
                .username("ana")
                .email("ana@mail.com")
                .balance(new BigDecimal("1500.00"))
                .build();

        when(userService.createUser("ana", "ana@mail.com", new BigDecimal("1500"))).thenReturn(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"email\":\"ana@mail.com\",\"balance\":1500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.email").value("ana@mail.com"))
                .andExpect(jsonPath("$.balance").value(1500.00));
    }
}
