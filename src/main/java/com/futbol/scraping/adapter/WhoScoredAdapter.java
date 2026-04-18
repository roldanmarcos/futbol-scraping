package com.futbol.scraping.adapter;

import com.futbol.scraping.dto.PlayerStatsDTO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * WhoScored adapter for scraping player statistics.
 * Note: WhoScored uses JavaScript rendering. This adapter attempts to scrape
 * basic player data; falls back to empty list when scraping fails (e.g., bot detection).
 */
@Component
@Slf4j
public class WhoScoredAdapter {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 15000;

    private static final Map<String, String> LEAGUE_URLS = Map.of(
            "Premier League", "https://www.whoscored.com/Regions/252/Tournaments/2/Seasons/10072/Stages/22706/PlayerStatistics",
            "Bundesliga", "https://www.whoscored.com/Regions/81/Tournaments/3/Seasons/10068/Stages/22702/PlayerStatistics",
            "La Liga", "https://www.whoscored.com/Regions/206/Tournaments/4/Seasons/10069/Stages/22703/PlayerStatistics",
            "Serie A", "https://www.whoscored.com/Regions/108/Tournaments/5/Seasons/10070/Stages/22704/PlayerStatistics",
            "Ligue 1", "https://www.whoscored.com/Regions/74/Tournaments/22/Seasons/10071/Stages/22705/PlayerStatistics"
    );

    public List<PlayerStatsDTO> scrapePlayersByLeague(String league) {
        log.info("Attempting to scrape WhoScored for league: {}", league);
        try {
            String url = LEAGUE_URLS.get(league);
            if (url == null) {
                log.warn("No WhoScored URL configured for league: {}", league);
                return Collections.emptyList();
            }

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .get();

            return parsePlayerStats(doc, league);
        } catch (Exception e) {
            log.warn("WhoScored scraping failed for league {}: {}. Using fallback data.", league, e.getMessage());
            return Collections.emptyList();
        }
    }

    public PlayerStatsDTO scrapePlayerById(String whoscoredId, String playerSlug) {
        String url = "https://www.whoscored.com/players/" + whoscoredId + "/matchstatistics/" + playerSlug;
        log.info("Attempting to scrape WhoScored player: {}", url);
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .get();
            return parsePlayerProfile(doc, whoscoredId, url);
        } catch (Exception e) {
            log.warn("WhoScored player scraping failed for id {}: {}", whoscoredId, e.getMessage());
            return null;
        }
    }

    private List<PlayerStatsDTO> parsePlayerStats(Document doc, String league) {
        List<PlayerStatsDTO> players = new ArrayList<>();
        try {
            Elements rows = doc.select("table#statistics-table-summary tbody tr");
            if (rows.isEmpty()) {
                rows = doc.select("tr.player-link");
            }

            for (Element row : rows) {
                try {
                    PlayerStatsDTO player = parseStatsRow(row, league);
                    if (player != null) {
                        players.add(player);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse row: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse WhoScored player stats table: {}", e.getMessage());
        }

        log.info("Parsed {} players from WhoScored for league {}", players.size(), league);
        return players;
    }

    private PlayerStatsDTO parseStatsRow(Element row, String league) {
        Elements cells = row.select("td");
        if (cells.size() < 8) return null;

        String playerName = cells.get(1).text();
        if (playerName.isBlank()) return null;

        Element playerLink = cells.get(1).selectFirst("a");
        String playerId = "";
        if (playerLink != null) {
            String href = playerLink.attr("href");
            String[] parts = href.split("/");
            if (parts.length > 2) {
                playerId = parts[2];
            }
        }

        return PlayerStatsDTO.builder()
                .whoscoredId("ws-" + (playerId.isEmpty() ? playerName.hashCode() : playerId))
                .name(playerName)
                .league(league)
                .team(cells.size() > 2 ? cells.get(2).text() : "Unknown")
                .position(cells.size() > 3 ? cells.get(3).text() : "MF")
                .appearances(parseIntSafe(cells.size() > 4 ? cells.get(4).text() : "0"))
                .goals(parseIntSafe(cells.size() > 5 ? cells.get(5).text() : "0"))
                .assists(parseIntSafe(cells.size() > 6 ? cells.get(6).text() : "0"))
                .rating(parseDoubleSafe(cells.size() > 7 ? cells.get(7).text() : "6.5"))
                .build();
    }

    private PlayerStatsDTO parsePlayerProfile(Document doc, String whoscoredId, String url) {
        try {
            Element nameEl = doc.selectFirst("h2.player-name");
            String name = nameEl != null ? nameEl.text() : "Unknown";

            return PlayerStatsDTO.builder()
                    .whoscoredId("ws-" + whoscoredId)
                    .name(name)
                    .url(url)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse player profile: {}", e.getMessage());
            return null;
        }
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 6.5;
        }
    }
}
