package com.futbol.scraping.web;

import com.futbol.scraping.dto.SyncPlayersResponse;
import com.futbol.scraping.service.ScrapingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Sincronización de jugadores y creación administrativa de usuarios")
public class SyncController {

    private final ScrapingService scrapingService;

    @PostMapping("/sync/players")
    @Operation(summary = "Sincronizar jugadores", description = "Actualiza los jugadores de una liga concreta o de todas las ligas si no se indica ninguna.")
    public ResponseEntity<SyncPlayersResponse> syncPlayers(
            @Parameter(description = "Liga a sincronizar; si no se envía, se sincronizan todas") @RequestParam(required = false) String league) {
        log.info("POST /sync/players - league={}", league);
        int count;
        if (league != null && !league.isBlank()) {
            count = scrapingService.syncLeague(league);
        } else {
            count = scrapingService.syncAllLeagues();
        }
        return ResponseEntity.ok(new SyncPlayersResponse(count, "SUCCESS"));
    }

}
