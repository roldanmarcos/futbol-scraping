package com.futbol.scraping;

import com.futbol.scraping.dto.AuthResponse;
import com.futbol.scraping.dto.RegisterRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.model.User;
import com.futbol.scraping.service.AuthService;
import com.futbol.scraping.service.JwtService;
import com.futbol.scraping.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegister_SuccessWithProvidedInitialBalance() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("  new-user  ");
        request.setEmail("  user@example.com  ");
        request.setPassword("secret123");
        request.setInitialBalance(new BigDecimal("1234.56"));

        User user = User.builder()
                .id(1L)
                .username("new-user")
                .email("user@example.com")
                .balance(new BigDecimal("1234.56"))
                .build();

        when(userService.createUser("new-user", "user@example.com", "secret123", new BigDecimal("1234.56")))
                .thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("new-user");
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getToken()).isEqualTo("jwt-token");

        verify(userService).createUser("new-user", "user@example.com", "secret123", new BigDecimal("1234.56"));
        verify(jwtService).generateToken(user);
    }

    @Test
    void testRegister_SuccessWithDefaultInitialBalance() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setEmail("user@example.com");
        request.setPassword("secret123");

        User user = User.builder()
                .id(2L)
                .username("new-user")
                .email("user@example.com")
                .balance(BigDecimal.valueOf(10_000))
                .build();

        when(userService.createUser("new-user", "user@example.com", "secret123", BigDecimal.valueOf(10_000)))
                .thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getToken()).isEqualTo("jwt-token");

        verify(userService).createUser("new-user", "user@example.com", "secret123", BigDecimal.valueOf(10_000));
        verify(jwtService).generateToken(user);
    }

    @Test
    void testRegister_RequestNull() {
        assertThatThrownBy(() -> authService.register(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Request body is required");

        verifyNoInteractions(userService, jwtService);
    }

    @Test
    void testRegister_UsernameBlank() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("   ");
        request.setEmail("user@example.com");
        request.setPassword("secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username is required");

        verifyNoInteractions(userService, jwtService);
    }

    @Test
    void testRegister_EmailBlank() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setEmail("   ");
        request.setPassword("secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email is required");

        verifyNoInteractions(userService, jwtService);
    }

    @Test
    void testRegister_PasswordTooShort() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setEmail("user@example.com");
        request.setPassword("12345");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password must be at least 6 characters");

        verifyNoInteractions(userService, jwtService);
    }
}

