package com.futbol.scraping;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.User;
import com.futbol.scraping.repository.UserRepository;
import com.futbol.scraping.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@FutbolUnit
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAssertUserMatchesOrSuperuser_TargetUserIdNull() {
        assertThatThrownBy(() -> authorizationService.assertUserMatchesOrSuperuser(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User id is required");

        verifyNoInteractions(userRepository);
    }

    @Test
    void testAssertUserMatchesOrSuperuser_NoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authorizationService.assertUserMatchesOrSuperuser(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication is required");

        verifyNoInteractions(userRepository);
    }

    @Test
    void testAssertUserMatchesOrSuperuser_AuthenticatedUserNotFound() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("missing-user", null));
        when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.assertUserMatchesOrSuperuser(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Authenticated user not found");

        verify(userRepository).findByUsername("missing-user");
    }

    @Test
    void testAssertUserMatchesOrSuperuser_SameUserAllowed() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("normal-user", null));

        User authenticatedUser = User.builder()
                .id(10L)
                .username("normal-user")
                .email("normal-user@example.com")
                .balance(BigDecimal.TEN)
                .isSuperuser(false)
                .build();

        when(userRepository.findByUsername("normal-user")).thenReturn(Optional.of(authenticatedUser));

        assertThatCode(() -> authorizationService.assertUserMatchesOrSuperuser(10L))
                .doesNotThrowAnyException();

        verify(userRepository).findByUsername("normal-user");
    }

    @Test
    void testAssertUserMatchesOrSuperuser_SuperuserAllowedForOtherUser() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));

        User authenticatedUser = User.builder()
                .id(99L)
                .username("admin")
                .email("admin@example.com")
                .balance(BigDecimal.TEN)
                .isSuperuser(true)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(authenticatedUser));

        assertThatCode(() -> authorizationService.assertUserMatchesOrSuperuser(1L))
                .doesNotThrowAnyException();

        verify(userRepository).findByUsername("admin");
    }

    @Test
    void testAssertUserMatchesOrSuperuser_NotOwnerAndNotSuperuser() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("normal-user", null));

        User authenticatedUser = User.builder()
                .id(10L)
                .username("normal-user")
                .email("normal-user@example.com")
                .balance(BigDecimal.TEN)
                .isSuperuser(false)
                .build();

        when(userRepository.findByUsername("normal-user")).thenReturn(Optional.of(authenticatedUser));

        assertThatThrownBy(() -> authorizationService.assertUserMatchesOrSuperuser(20L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access your own resources");

        verify(userRepository).findByUsername("normal-user");
    }
}

