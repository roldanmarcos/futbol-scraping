package com.futbol.scraping.web;

import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.PlayerDetailDTO;
import com.futbol.scraping.dto.PlayerRankingDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Players", description = "Consulta de jugadores, rankings y cotizaciones")
public class PlayerController {

    private final PlayerService playerService;
    private final QuoteService quoteService;

    @GetMapping
    @Operation(summary = "Listar jugadores", description = "Devuelve jugadores filtrando por liga, equipo o posición.")
    public ResponseEntity<List<PlayerDTO>> getPlayers(
            @Parameter(description = "Liga del jugador") @RequestParam(required = false) String league,
            @Parameter(description = "Equipo del jugador") @RequestParam(required = false) String team,
            @Parameter(description = "Posición del jugador") @RequestParam(required = false) String position) {
        log.debug("GET /players - league={}, team={}, position={}", league, team, position);
        return ResponseEntity.ok(playerService.getPlayers(league, team, position));
    }

    @GetMapping("/ranking")
    @Operation(summary = "Ranking de jugadores", description = "Devuelve el ranking calculado con la estrategia activa.")
    public ResponseEntity<List<PlayerRankingDTO>> getRanking() {
        log.debug("GET /players/ranking");
        return ResponseEntity.ok(quoteService.getRanking());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener jugador", description = "Devuelve el detalle completo de un jugador por su identificador.")
    public ResponseEntity<PlayerDetailDTO> getPlayer(
            @Parameter(description = "Identificador del jugador") @PathVariable Long id) {
        log.debug("GET /players/{}", id);
        return ResponseEntity.ok(playerService.getPlayerById(id));
    }

    @GetMapping("/{id}/quotes")
    @Operation(summary = "Historial de cotizaciones", description = "Devuelve el historial de cotizaciones o una cotización puntual si se envía fecha.")
    public ResponseEntity<List<QuoteDTO>> getPlayerQuotes(
            @Parameter(description = "Identificador del jugador") @PathVariable Long id,
            @Parameter(description = "Fecha y hora ISO-8601 para consultar una cotización puntual") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        log.debug("GET /players/{}/quotes - date={}", id, date);
        if (date != null) {
            return ResponseEntity.ok(List.of(quoteService.getQuoteAtDate(id, date)));
        }
        return ResponseEntity.ok(quoteService.getPlayerQuotes(id));
    }
}
