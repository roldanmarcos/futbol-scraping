package com.futbol.scraping.adapter;

import com.futbol.scraping.dto.PlayerStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootballDataAdapter {

    private final RestTemplate restTemplate;

    @Value("${football-data.api.base-url:https://api.football-data.org/v4}")
    private String baseUrl;

    @Value("${football-data.api.api-key:demo}")
    private String apiKey;

    private static final Map<String, String> LEAGUE_CODES = Map.of(
            "Premier League", "PL",
            "Bundesliga", "BL1",
            "La Liga", "PD",
            "Serie A", "SA",
            "Ligue 1", "FL1"
    );

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public List<PlayerStatsDTO> fetchPlayersByLeague(String league) {
        String competitionCode = LEAGUE_CODES.get(league);
        if (competitionCode == null) {
            log.warn("Unknown league: {}", league);
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = createHeaders();
            String url = baseUrl + "/competitions/" + competitionCode + "/scorers?limit=50";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getBody() == null) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scorers = (List<Map<String, Object>>) response.getBody().get("scorers");
            if (scorers == null) return Collections.emptyList();

            List<PlayerStatsDTO> players = new ArrayList<>();
            for (Map<String, Object> scorerEntry : scorers) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> playerData = (Map<String, Object>) scorerEntry.get("player");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> teamData = (Map<String, Object>) scorerEntry.get("team");
                    if (playerData == null) continue;

                    PlayerStatsDTO dto = PlayerStatsDTO.builder()
                            .whoscoredId("fd-" + playerData.get("id"))
                            .name((String) playerData.get("name"))
                            .team(teamData != null ? (String) teamData.get("name") : "Unknown")
                            .league(league)
                            .position(mapPosition((String) playerData.get("position")))
                            .nationality((String) playerData.get("nationality"))
                            .goals(toInt(scorerEntry.get("goals")))
                            .assists(toInt(scorerEntry.get("assists")))
                            .appearances(toInt(scorerEntry.get("playedMatches")))
                            .build();
                    players.add(dto);
                } catch (Exception e) {
                    log.warn("Failed to parse player entry: {}", e.getMessage());
                }
            }
            log.info("Fetched {} players from Football-Data.org for league {}", players.size(), league);
            return players;
        } catch (Exception e) {
            log.error("Failed to fetch players from Football-Data.org for league {}: {}", league, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Recover
    public List<PlayerStatsDTO> recoverFetchPlayers(Exception e, String league) {
        log.error("All retries exhausted for Football-Data.org league {}: {}", league, e.getMessage());
        return Collections.emptyList();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", apiKey);
        return headers;
    }

    private String mapPosition(String apiPosition) {
        if (apiPosition == null) return "MF";
        return switch (apiPosition) {
            case "Goalkeeper" -> "GK";
            case "Defence" -> "DF";
            case "Midfield" -> "MF";
            case "Offence" -> "FW";
            default -> "MF";
        };
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer i) return i;
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
