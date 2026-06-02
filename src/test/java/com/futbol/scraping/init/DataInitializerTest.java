package com.futbol.scraping.init;

import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.User;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.repository.PlayerTokenRepository;
import com.futbol.scraping.repository.UserRepository;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.service.ScrapingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    private UserRepository userRepository;
    private PlayerRepository playerRepository;
    private PlayerTokenRepository playerTokenRepository;
    private ScrapingService scrapingService;
    private QuoteService quoteService;
    private PasswordEncoder passwordEncoder;
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        playerRepository = mock(PlayerRepository.class);
        playerTokenRepository = mock(PlayerTokenRepository.class);
        scrapingService = mock(ScrapingService.class);
        quoteService = mock(QuoteService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        dataInitializer = new DataInitializer(
                userRepository,
                playerRepository,
                playerTokenRepository,
                scrapingService,
                quoteService,
                passwordEncoder);

        ReflectionTestUtils.setField(dataInitializer, "superuserUsername", "superuser");
        ReflectionTestUtils.setField(dataInitializer, "superuserEmail", "superuser@futbol.com");
        ReflectionTestUtils.setField(dataInitializer, "superuserInitialBalance", new BigDecimal("1000000"));
        ReflectionTestUtils.setField(dataInitializer, "superuserPassword", "superuser123");
        ReflectionTestUtils.setField(dataInitializer, "tokensPerPlayer", 100);
    }

    @Test
    void run_CreatesSuperuserSyncsAndAllocatesTokens() {
        User superuser = User.builder()
                .id(1L)
                .username("superuser")
                .email("superuser@futbol.com")
                .balance(new BigDecimal("1000000"))
                .isSuperuser(true)
                .build();
        Player player = Player.builder().id(5L).name("Player A").build();

        when(userRepository.findByUsername("superuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("superuser123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(superuser);
        when(playerRepository.count()).thenReturn(0L, 1L);
        when(scrapingService.syncAllLeagues()).thenReturn(12);
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(playerTokenRepository.findByPlayerAndUser(player, superuser)).thenReturn(Optional.empty());

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(scrapingService).syncAllLeagues();
        verify(playerTokenRepository).save(any());
        verify(quoteService).recalculate();
    }

    @Test
    void run_UpdatesBlankPasswordHashForExistingSuperuserAndSkipsSync() {
        User existing = User.builder()
                .id(10L)
                .username("superuser")
                .email("superuser@futbol.com")
                .passwordHash("   ")
                .balance(new BigDecimal("1000000"))
                .isSuperuser(true)
                .build();

        when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("superuser123")).thenReturn("updated-hash");
        when(userRepository.save(existing)).thenReturn(existing);
        when(playerRepository.count()).thenReturn(5L, 0L);
        when(playerRepository.findAll()).thenReturn(List.of());

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(userRepository).save(existing);
        verify(scrapingService, never()).syncAllLeagues();
        verify(quoteService, never()).recalculate();
    }

    @Test
    void run_DoesNotFail_WhenQuoteRecalculationThrows() {
        User existing = User.builder()
                .id(11L)
                .username("superuser")
                .email("superuser@futbol.com")
                .passwordHash("hash")
                .balance(new BigDecimal("1000000"))
                .isSuperuser(true)
                .build();

        when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(existing));
        when(playerRepository.count()).thenReturn(2L, 2L);
        when(playerRepository.findAll()).thenReturn(List.of());
        doThrow(new RuntimeException("boom")).when(quoteService).recalculate();

        dataInitializer.run(mock(ApplicationArguments.class));

        verify(quoteService, times(1)).recalculate();
    }
}
