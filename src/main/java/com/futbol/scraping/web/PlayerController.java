package com.futbol.scraping.web;

import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.PlayerDetailDTO;
import com.futbol.scraping.dto.PlayerRankingDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/players")
@RequiredArgsConstructor
@Slf4j
public class PlayerController {

    private final PlayerService playerService;
    private final QuoteService quoteService;

    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getPlayers(
            @RequestParam(required = false) String league,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String position) {
        log.debug("GET /players - league={}, team={}, position={}", league, team, position);
        return ResponseEntity.ok(playerService.getPlayers(league, team, position));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<PlayerRankingDTO>> getRanking() {
        log.debug("GET /players/ranking");
        return ResponseEntity.ok(quoteService.getRanking());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerDetailDTO> getPlayer(@PathVariable Long id) {
        log.debug("GET /players/{}", id);
        return ResponseEntity.ok(playerService.getPlayerById(id));
    }

    @GetMapping("/{id}/quotes")
    public ResponseEntity<List<QuoteDTO>> getPlayerQuotes(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        log.debug("GET /players/{}/quotes - date={}", id, date);
        if (date != null) {
            return ResponseEntity.ok(List.of(quoteService.getQuoteAtDate(id, date)));
        }
        return ResponseEntity.ok(quoteService.getPlayerQuotes(id));
    }
}
