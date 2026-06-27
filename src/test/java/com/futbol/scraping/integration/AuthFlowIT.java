package com.futbol.scraping.integration;

import com.futbol.scraping.annotation.FutbolIT;
import com.futbol.scraping.dto.AuthResponse;
import com.futbol.scraping.dto.RegisterRequest;
import com.futbol.scraping.repository.PlayerTokenRepository;
import com.futbol.scraping.repository.TransactionRepository;
import com.futbol.scraping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolIT
class AuthFlowIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerTokenRepository playerTokenRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        playerTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_createsUserInH2AndReturnsJwt() {
        RegisterRequest request = buildRequest("futbol_user", "futbol@test.com", "password123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("futbol_user");
        assertThat(response.getBody().getEmail()).isEqualTo("futbol@test.com");
        assertThat(response.getBody().getToken()).isNotEmpty();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(userRepository.findByUsername("futbol_user")).isPresent();
    }

    @Test
    void register_withDuplicateUsername_returns400() {
        RegisterRequest request = buildRequest("duplicado", "dup@test.com", "password123");
        restTemplate.postForEntity("/auth/register", request, String.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    void register_withPasswordTooShort_returns400() {
        RegisterRequest request = buildRequest("short_pass", "short@test.com", "123");

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.findByUsername("short_pass")).isEmpty();
    }

    @Test
    void register_withMissingUsername_returns400() {
        RegisterRequest request = buildRequest(null, "nousername@test.com", "password123");

        ResponseEntity<String> response = restTemplate.postForEntity("/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_thenAccessProtectedEndpoint_withValidJwt_returns200or404() {
        RegisterRequest request = buildRequest("jwt_user", "jwt@test.com", "password123");
        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", request, AuthResponse.class);
        String token = auth.getBody().getToken();
        Long userId = auth.getBody().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(
                "/users/" + userId + "/portfolio", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isIn(200, 404);
    }

    @Test
    void accessProtectedEndpoint_withoutJwt_returns401or403() {
        ResponseEntity<String> response = restTemplate.getForEntity("/users/1/portfolio", String.class);

        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    private RegisterRequest buildRequest(String username, String email, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }
}
