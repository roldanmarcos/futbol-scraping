package com.futbol.scraping.init;

import com.futbol.scraping.model.User;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.service.ScrapingService;
import com.futbol.scraping.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final PlayerService playerService;
    private final ScrapingService scrapingService;
    private final QuoteService quoteService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superuser.username:superuser}")
    private String superuserUsername;

    @Value("${app.superuser.email:superuser@futbol.com}")
    private String superuserEmail;

    @Value("${app.superuser.initial-balance:1000000}")
    private BigDecimal superuserInitialBalance;

    @Value("${app.superuser.password:superuser123}")
    private String superuserPassword;

    @Value("${app.tokens-per-player:100}")
    private int tokensPerPlayer;

    // Poner en false en application-test.yml para evitar que Selenium sea invocado en tests
    @Value("${app.data-initializer.enabled:true}")
    private boolean dataInitializerEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!dataInitializerEnabled) {
            log.info("DataInitializer deshabilitado (app.data-initializer.enabled=false)");
            return;
        }
        log.info("DataInitializer starting...");

        User superuser = createSuperuserIfNeeded();

        long playerCount = playerService.countPlayers();
        if (playerCount == 0) {
            log.info("No players found in database, syncing from external sources...");
            int synced = scrapingService.syncAllLeagues();
            log.info("Initial sync complete: {} players added", synced);
        } else {
            log.info("Database already has {} players, skipping initial sync", playerCount);
        }

        allocateTokensToSuperuser(superuser);

        if (playerService.countPlayers() > 0) {
            log.info("Calculating initial quotes...");
            try {
                quoteService.recalculate();
            } catch (Exception e) {
                log.error("Failed to calculate initial quotes: {}", e.getMessage());
            }
        }

        log.info("DataInitializer complete.");
    }

    private User createSuperuserIfNeeded() {
        return userService.findByUsername(superuserUsername)
                .map(existing -> {
                    if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()) {
                        existing.setPasswordHash(passwordEncoder.encode(superuserPassword));
                        log.info("Updating superuser password hash for existing user: {}", superuserUsername);
                        return userService.saveUser(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Creating superuser: {}", superuserUsername);
                    User superuser = User.builder()
                            .username(superuserUsername)
                            .email(superuserEmail)
                            .passwordHash(passwordEncoder.encode(superuserPassword))
                            .balance(superuserInitialBalance)
                            .isSuperuser(true)
                            .build();
                    return userService.saveUser(superuser);
                });
    }

    private void allocateTokensToSuperuser(User superuser) {
        userService.allocateTokens(superuser, tokensPerPlayer);
    }
}
