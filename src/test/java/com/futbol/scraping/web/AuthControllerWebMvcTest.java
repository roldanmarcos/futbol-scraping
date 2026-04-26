package com.futbol.scraping.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.scraping.config.JwtAuthenticationFilter;
import com.futbol.scraping.dto.AuthResponse;
import com.futbol.scraping.dto.RegisterRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.GlobalExceptionHandler;
import com.futbol.scraping.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private AuthService authService;

    @Test
    void register_ReturnsOk_WhenRequestIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ana");
        request.setEmail("ana@test.com");
        request.setPassword("secret1");
        request.setInitialBalance(new BigDecimal("15000.00"));

        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .username("ana")
                .email("ana@test.com")
                .token("jwt-token")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("ana");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@test.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("secret1");
        assertThat(captor.getValue().getInitialBalance()).isEqualByComparingTo("15000.00");
    }

    @Test
    void register_Returns400_WhenBusinessRuleFails() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ana");
        request.setEmail("ana@test.com");
        request.setPassword("123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("Password must be at least 6 characters"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must be at least 6 characters"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void register_Returns400_WhenJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"ana\", \"email\": \"ana@test.com\", \"password\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

