package com.futbol.scraping.web;

import com.futbol.scraping.dto.PortfolioDTO;
import com.futbol.scraping.dto.TransactionDTO;
import com.futbol.scraping.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}/portfolio")
    public ResponseEntity<PortfolioDTO> getPortfolio(@PathVariable Long id) {
        log.debug("GET /users/{}/portfolio", id);
        return ResponseEntity.ok(userService.getPortfolio(id));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable Long id) {
        log.debug("GET /users/{}/transactions", id);
        return ResponseEntity.ok(userService.getTransactions(id));
    }
}
