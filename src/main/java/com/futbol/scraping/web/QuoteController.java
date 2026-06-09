package com.futbol.scraping.web;

import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quotes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quotes", description = "Recálculo y consulta de cotizaciones")
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/recalculate")
    @Operation(summary = "Recalcular cotizaciones", description = "Ejecuta el cálculo de cotizaciones usando la estrategia activa o la indicada.")
    public ResponseEntity<RecalculateResponse> recalculate(
            @Parameter(description = "Estrategia de cotización a usar") @RequestParam(required = false) String strategy) {
        log.info("POST /quotes/recalculate - strategy={}", strategy);
        if (strategy != null && !strategy.isBlank()) {
            quoteService.setActiveStrategy(strategy);
        }
        return ResponseEntity.ok(quoteService.recalculate());
    }
}
