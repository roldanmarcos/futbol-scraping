package com.futbol.scraping.web;

import com.futbol.scraping.service.ScrapingService;
import com.futbol.scraping.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SyncController {

    private final ScrapingService scrapingService;
    private final UserService userService;

    @PostMapping("/sync/players")
    public ResponseEntity<Map<String, Object>> syncPlayers(
            @RequestParam(required = false) String league) {
        log.info("POST /sync/players - league={}", league);
        int count;
        if (league != null && !league.isBlank()) {
            count = scrapingService.syncLeague(league);
        } else {
            count = scrapingService.syncAllLeagues();
        }
        return ResponseEntity.ok(Map.of("playersSync", count, "status", "SUCCESS"));
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String email = (String) body.get("email");
        
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        Object balanceObj = body.get("balance");
        BigDecimal balance;
        try {
            balance = balanceObj != null
                    ? new BigDecimal(balanceObj.toString())
                    : BigDecimal.valueOf(10000);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid balance format: " + balanceObj);
        }

        var user = userService.createUser(username, email, balance);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "balance", user.getBalance()
        ));
    }
}
