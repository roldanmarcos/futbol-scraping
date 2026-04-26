package com.futbol.scraping.ControllerTest;

import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.service.ScrapingService;
import com.futbol.scraping.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.scraping.web.SyncController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SyncController.class)
@AutoConfigureMockMvc(addFilters = false)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScrapingService scrapingService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Sync Players Tests ====================

    @Test
    void testSyncPlayers_WithoutLeague() throws Exception {
        when(scrapingService.syncAllLeagues()).thenReturn(150);

        mockMvc.perform(post("/sync/players"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.playersSync").value(150))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testSyncPlayers_WithValidLeague() throws Exception {
        String league = "La Liga";
        when(scrapingService.syncLeague(league)).thenReturn(80);

        mockMvc.perform(post("/sync/players?league=La Liga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersSync").value(80))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testSyncPlayers_WithBlankLeague() throws Exception {
        when(scrapingService.syncAllLeagues()).thenReturn(150);

        mockMvc.perform(post("/sync/players?league=   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersSync").value(150))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testSyncPlayers_WithEmptyLeague() throws Exception {
        when(scrapingService.syncAllLeagues()).thenReturn(150);

        mockMvc.perform(post("/sync/players?league="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersSync").value(150))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testSyncPlayers_ZeroPlayersProcessed() throws Exception {
        when(scrapingService.syncAllLeagues()).thenReturn(0);

        mockMvc.perform(post("/sync/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersSync").value(0));
    }

    @Test
    void testSyncPlayers_LargeDataset() throws Exception {
        when(scrapingService.syncAllLeagues()).thenReturn(10000);

        mockMvc.perform(post("/sync/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playersSync").value(10000));
    }

    @Test
    void testSyncPlayers_LeagueNotFound() throws Exception {
        String league = "NonExistentLeague";
        when(scrapingService.syncLeague(league))
                .thenThrow(new BusinessException("League not found"));

        mockMvc.perform(post("/sync/players?league=" + league))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSyncPlayers_SyncError() throws Exception {
        when(scrapingService.syncAllLeagues())
                .thenThrow(new BusinessException("Network error during sync"));

        mockMvc.perform(post("/sync/players"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Create User Tests ====================

    @Test
    void testCreateUser_Success() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "john_doe",
                "email", "john@example.com",
                "balance", 5000
        );

        com.futbol.scraping.model.User user = com.futbol.scraping.model.User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .balance(new BigDecimal("5000"))
                .isSuperuser(false)
                .build();

        when(userService.createUser("john_doe", "john@example.com", new BigDecimal("5000")))
                .thenReturn(user);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.balance").value(5000.0));
    }

    @Test
    void testCreateUser_DefaultBalance() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "jane_doe",
                "email", "jane@example.com"
        );

        com.futbol.scraping.model.User user = com.futbol.scraping.model.User.builder()
                .id(2L)
                .username("jane_doe")
                .email("jane@example.com")
                .balance(new BigDecimal("10000"))
                .isSuperuser(false)
                .build();

        when(userService.createUser("jane_doe", "jane@example.com", new BigDecimal("10000")))
                .thenReturn(user);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000.0));
    }

    @Test
    void testCreateUser_CustomBalance() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "bob",
                "email", "bob@example.com",
                "balance", 20000
        );

        com.futbol.scraping.model.User user = com.futbol.scraping.model.User.builder()
                .id(3L)
                .username("bob")
                .email("bob@example.com")
                .balance(new BigDecimal("20000"))
                .isSuperuser(false)
                .build();

        when(userService.createUser("bob", "bob@example.com", new BigDecimal("20000")))
                .thenReturn(user);

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(20000.0));
    }

    @Test
    void testCreateUser_MissingUsername() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "test@example.com",
                "balance", 5000
        );

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_MissingEmail() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "testuser",
                "balance", 5000
        );

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_InvalidBalance() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "testuser",
                "email", "test@example.com",
                "balance", "invalid"
        );

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_NegativeBalance() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "testuser",
                "email", "test@example.com",
                "balance", -5000
        );

        when(userService.createUser("testuser", "test@example.com", new BigDecimal("-5000")))
                .thenThrow(new BusinessException("Balance cannot be negative"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_DuplicateUsername() throws Exception {
        Map<String, Object> request = Map.of(
                "username", "john_doe",
                "email", "john2@example.com"
        );

        when(userService.createUser("john_doe", "john2@example.com", new BigDecimal("10000")))
                .thenThrow(new BusinessException("Username already exists"));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
