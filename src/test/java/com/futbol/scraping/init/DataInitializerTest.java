package com.futbol.scraping.init;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.model.User;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.service.ScrapingService;
import com.futbol.scraping.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@FutbolUnit
class DataInitializerTest {

    private UserService userService;
    private PlayerService playerService;
    private ScrapingService scrapingService;
    private QuoteService quoteService;
    private PasswordEncoder passwordEncoder;
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        playerService = mock(PlayerService.class);
        scrapingService = mock(ScrapingService.class);
        quoteService = mock(QuoteService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        dataInitializer = new DataInitializer(
                userService, playerService,
                scrapingService, quoteService, passwordEncoder);

        ReflectionTestUtils.setField(dataInitializer, "dataInitializerEnabled", true);
        ReflectionTestUtils.setField(dataInitializer, "superuserUsername", "superuser");
        ReflectionTestUtils.setField(dataInitializer, "superuserEmail", "superuser@futbol.com");
        ReflectionTestUtils.setField(dataInitializer, "superuserInitialBalance", new BigDecimal("1000000"));
        ReflectionTestUtils.setField(dataInitializer, "superuserPassword", "superuser123");
        ReflectionTestUtils.setField(dataInitializer, "tokensPerPlayer", 100);
    }

    @Test
    void run_CreatesSuperuserSyncsAndAllocatesTokens() {
        User superuser = User.builder()
                .id(1L).username("superuser").email("superuser@futbol.com")
                .balance(new BigDecimal("1000000")).isSuperuser(true).build();

        when(userService.findByUsername("superuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("superuser123")).thenReturn("encoded-password");
        when(userService.saveUser(any(User.class))).thenReturn(superuser);
        when(playerService.countPlayers()).thenReturn(0L, 1L);
        when(scrapingService.syncAllLeagues()).thenReturn(12);

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(scrapingService).syncAllLeagues();
        verify(userService).allocateTokens(superuser, 100);
        verify(quoteService).recalculate();
    }

    @Test
    void run_UpdatesBlankPasswordHashForExistingSuperuserAndSkipsSync() {
        User existing = User.builder()
                .id(10L).username("superuser").email("superuser@futbol.com")
                .passwordHash("   ").balance(new BigDecimal("1000000")).isSuperuser(true).build();

        when(userService.findByUsername("superuser")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("superuser123")).thenReturn("updated-hash");
        when(userService.saveUser(existing)).thenReturn(existing);
        when(playerService.countPlayers()).thenReturn(5L, 0L);

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(userService).saveUser(existing);
        verify(scrapingService, never()).syncAllLeagues();
        verify(quoteService, never()).recalculate();
    }

    @Test
    void run_DoesNotFail_WhenQuoteRecalculationThrows() {
        User existing = User.builder()
                .id(11L).username("superuser").email("superuser@futbol.com")
                .passwordHash("hash").balance(new BigDecimal("1000000")).isSuperuser(true).build();

        when(userService.findByUsername("superuser")).thenReturn(Optional.of(existing));
        when(playerService.countPlayers()).thenReturn(2L, 2L);
        doThrow(new RuntimeException("boom")).when(quoteService).recalculate();

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(quoteService, times(1)).recalculate();
    }
}
