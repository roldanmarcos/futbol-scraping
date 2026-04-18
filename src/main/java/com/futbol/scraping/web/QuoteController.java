package com.futbol.scraping.web;

import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/recalculate")
    public ResponseEntity<RecalculateResponse> recalculate(
            @RequestParam(required = false) String strategy) {
        log.info("POST /quotes/recalculate - strategy={}", strategy);
        if (strategy != null && !strategy.isBlank()) {
            quoteService.setActiveStrategy(strategy);
        }
        return ResponseEntity.ok(quoteService.recalculate());
    }
}
