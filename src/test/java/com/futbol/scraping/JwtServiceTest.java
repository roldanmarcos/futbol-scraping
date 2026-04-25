package com.futbol.scraping;

import com.futbol.scraping.model.User;
import com.futbol.scraping.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
        testUser = User.builder()
                .id(1L)
                .username("jwt-user")
                .email("jwt-user@example.com")
                .balance(BigDecimal.TEN)
                .isSuperuser(false)
                .build();
    }

    @Test
    void testGenerateTokenAndExtractUsername() {
        String token = jwtService.generateToken(testUser);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("jwt-user");
    }

    @Test
    void testIsTokenValid_SameUserAndNotExpired() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    void testIsTokenValid_DifferentUser() {
        String token = jwtService.generateToken(testUser);

        User otherUser = User.builder()
                .id(2L)
                .username("other-user")
                .email("other-user@example.com")
                .balance(BigDecimal.ONE)
                .isSuperuser(false)
                .build();

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertThat(isValid).isFalse();
    }
//
//    @Test
//    void testIsTokenValid_ExpiredToken() {
//        JwtService expiredJwtService = new JwtService(SECRET, -1);
//        String token = expiredJwtService.generateToken(testUser);
//
//        boolean isValid = expiredJwtService.isTokenValid(token, testUser);
//
//        assertThat(isValid).isFalse();
//    }

    @Test
    void testExtractUsername_InvalidToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-valid-jwt"))
                .isInstanceOf(JwtException.class);
    }
}

