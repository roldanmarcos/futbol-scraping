package com.futbol.scraping.config;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.model.User;
import com.futbol.scraping.repository.UserRepository;
import com.futbol.scraping.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@FutbolUnit
class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserRepository userRepository;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthenticationFilter(jwtService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_SkipsWhenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilterInternal_SetsUserRole_WhenTokenIsValidForRegularUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        User user = User.builder()
                .id(1L)
                .username("regular")
                .email("regular@futbol.com")
                .balance(BigDecimal.ONE)
                .isSuperuser(false)
                .build();

        when(jwtService.extractUsername("token-123")).thenReturn("regular");
        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("token-123", user)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("regular");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void doFilterInternal_SetsAdminAndUserRoles_WhenTokenIsValidForSuperuser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        User admin = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@futbol.com")
                .balance(BigDecimal.TEN)
                .isSuperuser(true)
                .build();

        when(jwtService.extractUsername("admin-token")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(jwtService.isTokenValid("admin-token", admin)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void doFilterInternal_DoesNotAuthenticate_WhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        User user = User.builder()
                .id(3L)
                .username("user")
                .email("user@futbol.com")
                .balance(BigDecimal.ONE)
                .isSuperuser(false)
                .build();

        when(jwtService.extractUsername("invalid-token")).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("invalid-token", user)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_DoesNotThrow_WhenJwtServiceFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername("broken-token")).thenThrow(new JwtException("bad token"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
