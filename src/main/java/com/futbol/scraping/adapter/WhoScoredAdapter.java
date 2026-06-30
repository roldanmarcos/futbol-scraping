package com.futbol.scraping.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.scraping.dto.PlayerStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * WhoScored adapter for scraping player statistics.
 * Note: WhoScored uses JavaScript rendering. This adapter attempts to scrape
 * basic player data; falls back to empty list when scraping fails (e.g., bot
 * detection).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WhoScoredAdapter {

    private final ObjectMapper objectMapper;

    @Value("${whoscored.number-of-players:2757}")
    private int numberOfPlayers;

    @Value("${whoscored.selenium.headless:false}")
    private boolean seleniumHeadless;

    @Value("${whoscored.selenium.wait-seconds:20}")
    private int waitSeconds;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final String PREMIER_LEAGUE = "Premier League";
    private static final String BUNDESLIGA = "Bundesliga";
    private static final String LA_LIGA = "La Liga";
    private static final String SERIE_A = "Serie A";
    private static final String LIGUE_1 = "Ligue 1";
    private static final Map<String, String> LEAGUE_PAGE_URLS = Map.of(
            PREMIER_LEAGUE,
            "https://es.whoscored.com/Regions/182/Tournaments/77/Seasons/10764/Stages/24555/PlayerStatistics",
            BUNDESLIGA,
            "https://es.whoscored.com/Regions/81/Tournaments/3/Seasons/10720/Stages/24478/PlayerStatistics",
            LA_LIGA, "https://es.whoscored.com/Regions/206/Tournaments/4/Seasons/10803/Stages/24622/PlayerStatistics",
            SERIE_A, "https://es.whoscored.com/Regions/108/Tournaments/5/Seasons/10732/Stages/24500/PlayerStatistics",
            LIGUE_1,
            "https://es.whoscored.com/Regions/74/Tournaments/22/Seasons/10792/Stages/24609/PlayerStatistics");

    private static final Map<String, String> LEAGUE_TOURNAMENT_IDS = Map.of(
            PREMIER_LEAGUE, "77",
            BUNDESLIGA, "3",
            LA_LIGA, "4",
            SERIE_A, "5",
            LIGUE_1, "22");

    private static final Map<String, String> LEAGUE_STAGE_IDS = Map.of(
            PREMIER_LEAGUE, "24555",
            BUNDESLIGA, "24478",
            LA_LIGA, "24622",
            SERIE_A, "24500",
            LIGUE_1, "24609"
    );

    public List<PlayerStatsDTO> scrapePlayersByLeague(String league) {
        log.info("Fetching WhoScored statistics with Selenium for league: {}", league);
        try {
            String tournamentId = LEAGUE_TOURNAMENT_IDS.get(league);
            if (tournamentId == null) {
                log.warn("No tournament id configured for league: {}", league);
                return Collections.emptyList();
            }

            String pageUrl = LEAGUE_PAGE_URLS.get(league);
            if (pageUrl == null) {
                log.warn("No WhoScored page url configured for league: {}", league);
                return Collections.emptyList();
            }

            String stageId = LEAGUE_STAGE_IDS.get(league);
                if (stageId == null) {
                    log.warn("No stage id configured for league: {}", league);
                    return Collections.emptyList();
                }

            WebDriver driver = null;
            try {
                driver = createDriver();
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
                driver.get(pageUrl);

                new WebDriverWait(driver, Duration.ofSeconds(waitSeconds)).until(d -> {
                    Object state = ((JavascriptExecutor) d).executeScript("return document.readyState");
                    return "complete".equals(state);
                });

                String feedBody = fetchFeedBodyInBrowser(driver, tournamentId, stageId, numberOfPlayers);
                if (feedBody == null || feedBody.isBlank()) {
                    return Collections.emptyList();
                }
                return parsePlayerStats(feedBody, league);
            } finally {
                if (driver != null) {
                    driver.quit();
                }
            }
        } catch (Exception e) {
            log.warn("WhoScored Selenium scraping failed for league {}: {}", league, e.getMessage());
            return Collections.emptyList();
        }
    }

    private WebDriver createDriver() {
        ChromeOptions options = new ChromeOptions();
        if (seleniumHeadless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=es-ES");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--user-agent=" + USER_AGENT);
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);
        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        return driver;
    }

    private String fetchFeedBodyInBrowser(WebDriver driver, String tournamentId, String stageId, int playersToPick) {
        String result = (String) ((JavascriptExecutor) driver).executeAsyncScript(
                "const tournamentId = arguments[0];"
                        + "const stageId = arguments[1];"
                        + "const playersToPick = arguments[2];"
                        + "const done = arguments[arguments.length - 1];"
                        + "const params = new URLSearchParams({"
                        + "category:'summary', subcategory:'all', statsAccumulationType:'0', isCurrent:'true',"
                        + "playerId:'', teamIds:'', matchId:'', stageId:stageId, tournamentOptions:tournamentId,"
                        + "sortBy:'Rating', sortAscending:'', age:'', ageComparisonType:'',"
                        + "appearances:'', appearancesComparisonType:'', field:'Overall',"
                        + "positionOptions:'', timeOfTheGameEnd:'', timeOfTheGameStart:'', isMinApp:'true',"
                        + "page:'', includeZeroValues:'', numberOfPlayersToPick:String(playersToPick), incPens:''"
                        + "});"
                        + "fetch('https://es.whoscored.com/statisticsfeed/1/getplayerstatistics?' + params.toString(), {credentials: 'include'})"
                        + ".then(async (response) => {"
                        + "  const body = await response.text();"
                        + "  done(JSON.stringify({status: response.status, body: body}));"
                        + "})"
                        + ".catch((error) => done(JSON.stringify({status: 0, error: String(error)})));",
                tournamentId,
                stageId,
                playersToPick);

        try {
            JsonNode wrapper = objectMapper.readTree(result);
            int status = wrapper.path("status").asInt(0);
            if (status != 200) {
                log.warn("WhoScored browser fetch failed with status {} for tournament {}", status, tournamentId);
                return null;
            }
            return wrapper.path("body").asText("");
        } catch (Exception e) {
            log.warn("Failed to parse WhoScored browser fetch wrapper: {}", e.getMessage());
            return null;
        }
    }

    public PlayerStatsDTO scrapePlayerById(String whoscoredId) {
        log.info("scrapePlayerById is not implemented for statistics feed mode: {}", whoscoredId);
        return null;
    }

    private List<PlayerStatsDTO> parsePlayerStats(String body, String league) {
        List<PlayerStatsDTO> players = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode rows;
            if (root.isArray()) {
                rows = root;
            } else if (root.has("playerTableStats")) {
                rows = root.get("playerTableStats");
            } else if (root.has("statistics")) {
                rows = root.get("statistics");
            } else {
                rows = objectMapper.createArrayNode();
            }

            for (JsonNode row : rows) {
                parseAndAddPlayerStats(league, players, row);
            }
        } catch (Exception e) {
            log.warn("Failed to parse WhoScored player stats table: {}", e.getMessage());
        }

        log.info("Parsed {} players from WhoScored for league {}", players.size(), league);
        return players;
    }

    private void parseAndAddPlayerStats(String league, List<PlayerStatsDTO> players, JsonNode row) {
        try {
            PlayerStatsDTO player = parseStatsRow(row, league);
            if (player != null) {
                players.add(player);
            }
        } catch (Exception e) {
            log.debug("Failed to parse row: {}", e.getMessage());
        }
    }

    private PlayerStatsDTO parseStatsRow(JsonNode row, String fallbackLeague) {
        String name = text(row, "name");
        Long playerId = longValue(row, "playerId");
        if ((name == null || name.isBlank()) && playerId == null) {
            return null;
        }

        String league = normalizeLeagueName(text(row, "tournamentName"));
        if (league == null || league.isBlank()) {
            league = fallbackLeague;
        }

        String whoscoredId = playerId != null ? "ws-" + playerId : null;
        String playerUrl = playerId != null ? "https://es.whoscored.com/Players/" + playerId + "/Show" : null;

        return PlayerStatsDTO.builder()
                .whoscoredId(whoscoredId)
                .playerId(playerId)
                .name(name)
                .firstName(text(row, "firstName"))
                .lastName(text(row, "lastName"))
                .team(text(row, "teamName"))
                .teamId(longValue(row, "teamId"))
                .league(league)
                .position(mapPosition(text(row, "positionText"), text(row, "playedPositionsShort")))
                .positionText(text(row, "positionText"))
                .playedPositions(text(row, "playedPositions"))
                .playedPositionsShort(text(row, "playedPositionsShort"))
                .nationality(text(row, "nationality"))
                .teamRegionName(text(row, "teamRegionName"))
                .regionCode(text(row, "regionCode"))
                .age(intValue(row, "age"))
                .height(intValue(row, "height"))
                .weight(intValue(row, "weight"))
                .appearances(intValue(row, "apps"))
                .subOn(intValue(row, "subOn"))
                .manOfTheMatch(intValue(row, "manOfTheMatch"))
                .goals(intValue(row, "goal"))
                .assists(intValue(row, "assistTotal"))
                .shotsPerGame(doubleValue(row, "shotsPerGame"))
                .aerialWonPerGame(doubleValue(row, "aerialWonPerGame"))
                .rating(doubleValue(row, "rating"))
                .minutesPlayed(intValue(row, "minsPlayed"))
                .yellowCard(doubleValue(row, "yellowCard"))
                .redCard(doubleValue(row, "redCard"))
                .passSuccess(doubleValue(row, "passSuccess"))
                .ranking(intValue(row, "ranking"))
                .isManOfTheMatch(boolValue(row, "isManOfTheMatch"))
                .isActive(boolValue(row, "isActive"))
                .isOpta(boolValue(row, "isOpta"))
                .tournamentShortName(text(row, "tournamentShortName"))
                .tournamentId(longValue(row, "tournamentId"))
                .tournamentName(text(row, "tournamentName"))
                .tournamentRegionId(longValue(row, "tournamentRegionId"))
                .tournamentRegionCode(text(row, "tournamentRegionCode"))
                .tournamentRegionName(text(row, "tournamentRegionName"))
                .seasonId(longValue(row, "seasonId"))
                .seasonName(text(row, "seasonName"))
                .url(playerUrl)
                .build();
    }

    private String normalizeLeagueName(String tournamentName) {
        if (tournamentName == null || tournamentName.isBlank()) {
            return tournamentName;
        }
        return switch (tournamentName.trim().toLowerCase()) {
            case "laliga", "la liga" -> LA_LIGA;
            case "premier league" -> PREMIER_LEAGUE;
            case "bundesliga" -> BUNDESLIGA;
            case "serie a" -> SERIE_A;
            case "ligue 1" -> LIGUE_1;
            default -> tournamentName;
        };
    }

    private String mapPosition(String positionText, String playedPositionsShort) {
        String mappedFromText = mapPositionFromText(positionText);
        if (mappedFromText != null) {
            return mappedFromText;
        }

        String mappedFromShortCode = mapPositionFromShortCode(playedPositionsShort);
        if (mappedFromShortCode != null) {
            return mappedFromShortCode;
        }

        return "MF";
    }

    private String mapPositionFromText(String positionText) {
        String normalized = positionText != null ? positionText.trim().toLowerCase() : "";
        if (normalized.contains("goalkeeper")) {
            return "GK";
        }
        if (normalized.contains("defender")) {
            return "DF";
        }
        if (normalized.contains("midfielder")) {
            return "MF";
        }
        if (normalized.contains("forward") || normalized.contains("striker")) {
            return "FW";
        }
        return null;
    }

    private String mapPositionFromShortCode(String playedPositionsShort) {
        if (playedPositionsShort == null || playedPositionsShort.isBlank()) {
            return null;
        }
        if (playedPositionsShort.contains("GK")) {
            return "GK";
        }
        if (playedPositionsShort.contains("D")) {
            return "DF";
        }
        if (playedPositionsShort.contains("M")) {
            return "MF";
        }
        if (playedPositionsShort.contains("F")) {
            return "FW";
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private Integer intValue(JsonNode node, String field) {
        try {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asInt();
        } catch (Exception e) {
            return null;
        }
    }

    private Long longValue(JsonNode node, String field) {
        try {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asLong();
        } catch (Exception e) {
            return null;
        }
    }

    private Double doubleValue(JsonNode node, String field) {
        try {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asDouble();
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean boolValue(JsonNode node, String field) {
        try {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                return Boolean.FALSE;
            }
            return value.asBoolean();
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }
}
