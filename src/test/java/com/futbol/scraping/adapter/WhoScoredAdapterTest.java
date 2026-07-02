package com.futbol.scraping.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.dto.PlayerStatsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
@ExtendWith(MockitoExtension.class)
class WhoScoredAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WhoScoredAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new WhoScoredAdapter(objectMapper);
        ReflectionTestUtils.setField(adapter, "numberOfPlayers", 10);
        ReflectionTestUtils.setField(adapter, "seleniumHeadless", true);
        ReflectionTestUtils.setField(adapter, "waitSeconds", 5);
    }

    @Test
    void scrapePlayersByLeague_UnknownLeague_ReturnsEmpty() {
        List<PlayerStatsDTO> result = adapter.scrapePlayersByLeague("Unknown League");
        assertThat(result).isEmpty();
    }

    @Test
    void scrapePlayerById_ReturnsNull() {
        PlayerStatsDTO result = adapter.scrapePlayerById("12345");
        assertThat(result).isNull();
    }

    @Test
    void parsePlayerStats_WithEmptyBody_ReturnsEmptyList() {
        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", "[]", "Premier League");

        assertThat(result).isEmpty();
    }

    @Test
    void parsePlayerStats_WithArrayBody_ReturnsPlayers() throws Exception {
        ObjectNode playerNode = createMinimalPlayerNode("Test Player", 1L);
        ArrayNode array = objectMapper.createArrayNode();
        array.add(playerNode);
        String json = objectMapper.writeValueAsString(array);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Player");
    }

    @Test
    void parsePlayerStats_WithPlayerTableStatsNode_ReturnsPlayers() throws Exception {
        ObjectNode playerNode = createMinimalPlayerNode("Messi", 10L);
        ArrayNode array = objectMapper.createArrayNode();
        array.add(playerNode);
        ObjectNode root = objectMapper.createObjectNode();
        root.set("playerTableStats", array);
        String json = objectMapper.writeValueAsString(root);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "La Liga");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Messi");
    }

    @Test
    void parsePlayerStats_WithStatisticsNode_ReturnsPlayers() throws Exception {
        ObjectNode playerNode = createMinimalPlayerNode("Ronaldo", 7L);
        ArrayNode array = objectMapper.createArrayNode();
        array.add(playerNode);
        ObjectNode root = objectMapper.createObjectNode();
        root.set("statistics", array);
        String json = objectMapper.writeValueAsString(root);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Serie A");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Ronaldo");
    }

    @Test
    void parsePlayerStats_WithMalformedJson_ReturnsEmptyList() {
        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", "{invalid json}", "Premier League");

        assertThat(result).isEmpty();
    }

    @Test
    void parsePlayerStats_WithRowMissingNameAndPlayerId_SkipsRow() throws Exception {
        ObjectNode invalidRow = objectMapper.createObjectNode();
        ArrayNode array = objectMapper.createArrayNode();
        array.add(invalidRow);

        ObjectNode validRow = createMinimalPlayerNode("Valid Player", 2L);
        array.add(validRow);

        String json = objectMapper.writeValueAsString(array);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Valid Player");
    }

    @Test
    void parsePlayerStats_WithFullPlayerData_ParsesAllFields() throws Exception {
        ObjectNode playerNode = objectMapper.createObjectNode();
        playerNode.put("name", "Cristiano Ronaldo");
        playerNode.put("playerId", 999L);
        playerNode.put("firstName", "Cristiano");
        playerNode.put("lastName", "Ronaldo");
        playerNode.put("teamName", "Al Nassr");
        playerNode.put("teamId", 456L);
        playerNode.put("positionText", "Forward");
        playerNode.put("playedPositions", "ST, LW");
        playerNode.put("playedPositionsShort", "ST, LW");
        playerNode.put("nationality", "Portugal");
        playerNode.put("teamRegionName", "Saudi Arabia");
        playerNode.put("regionCode", "SA");
        playerNode.put("age", 39);
        playerNode.put("height", 187);
        playerNode.put("weight", 85);
        playerNode.put("apps", 20);
        playerNode.put("subOn", 2);
        playerNode.put("manOfTheMatch", 3);
        playerNode.put("goal", 15);
        playerNode.put("assistTotal", 5);
        playerNode.put("shotsPerGame", 3.5);
        playerNode.put("aerialWonPerGame", 2.1);
        playerNode.put("rating", 8.5);
        playerNode.put("minsPlayed", 1800);
        playerNode.put("yellowCard", 1.0);
        playerNode.put("redCard", 0.0);
        playerNode.put("passSuccess", 82.5);
        playerNode.put("ranking", 1);
        playerNode.put("isManOfTheMatch", true);
        playerNode.put("isActive", true);
        playerNode.put("isOpta", true);
        playerNode.put("tournamentShortName", "SPL");
        playerNode.put("tournamentId", 10L);
        playerNode.put("tournamentName", "Saudi Pro League");
        playerNode.put("tournamentRegionId", 5L);
        playerNode.put("tournamentRegionCode", "SA");
        playerNode.put("tournamentRegionName", "Saudi Arabia");
        playerNode.put("seasonId", 2024L);
        playerNode.put("seasonName", "2024/2025");

        ArrayNode array = objectMapper.createArrayNode();
        array.add(playerNode);
        String json = objectMapper.writeValueAsString(array);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result).hasSize(1);
        PlayerStatsDTO p = result.get(0);
        assertThat(p.getName()).isEqualTo("Cristiano Ronaldo");
        assertThat(p.getFirstName()).isEqualTo("Cristiano");
        assertThat(p.getLastName()).isEqualTo("Ronaldo");
        assertThat(p.getTeamId()).isEqualTo(456L);
        assertThat(p.getAge()).isEqualTo(39);
        assertThat(p.getHeight()).isEqualTo(187);
        assertThat(p.getWeight()).isEqualTo(85);
        assertThat(p.getAppearances()).isEqualTo(20);
        assertThat(p.getSubOn()).isEqualTo(2);
        assertThat(p.getManOfTheMatch()).isEqualTo(3);
        assertThat(p.getMinutesPlayed()).isEqualTo(1800);
        assertThat(p.getGoals()).isEqualTo(15);
        assertThat(p.getAssists()).isEqualTo(5);
        assertThat(p.getShotsPerGame()).isEqualTo(3.5);
        assertThat(p.getAerialWonPerGame()).isEqualTo(2.1);
        assertThat(p.getRating()).isEqualTo(8.5);
        assertThat(p.getYellowCard()).isEqualTo(1.0);
        assertThat(p.getRedCard()).isEqualTo(0.0);
        assertThat(p.getPassSuccess()).isEqualTo(82.5);
        assertThat(p.getRanking()).isEqualTo(1);
        assertThat(p.getIsManOfTheMatch()).isTrue();
        assertThat(p.getIsActive()).isTrue();
        assertThat(p.getIsOpta()).isTrue();
        assertThat(p.getNationality()).isEqualTo("Portugal");
        assertThat(p.getWhoscoredId()).isEqualTo("ws-999");
        assertThat(p.getUrl()).isEqualTo("https://es.whoscored.com/Players/999/Show");
    }

    @Test
    void parsePlayerStats_WithMultiplePlayers() throws Exception {
        ObjectNode p1 = createMinimalPlayerNode("Player 1", 1L);
        ObjectNode p2 = createMinimalPlayerNode("Player 2", 2L);
        ObjectNode p3 = createMinimalPlayerNode("Player 3", 3L);

        ArrayNode array = objectMapper.createArrayNode();
        array.add(p1);
        array.add(p2);
        array.add(p3);
        String json = objectMapper.writeValueAsString(array);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "La Liga");

        assertThat(result).hasSize(3);
    }

    @Test
    void normalizeLeagueName_WithKnownNames() {
        assertThat((String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", "LaLiga")).isEqualTo("La Liga");
        assertThat((String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", "la liga")).isEqualTo("La Liga");
        assertThat((String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", "Premier League")).isEqualTo("Premier League");
        assertThat((String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", "Bundesliga")).isEqualTo("Bundesliga");
        assertThat((String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", "Serie A")).isEqualTo("Serie A");
        assertThat((String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", "Ligue 1")).isEqualTo("Ligue 1");
    }

    @Test
    void normalizeLeagueName_WithNull_ReturnsNull() {
        String result = (String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", new Object[] { null });
        assertThat(result).isNull();
    }

    @Test
    void normalizeLeagueName_WithBlank_ReturnsBlank() {
        String result = (String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", " ");
        assertThat(result).isEqualTo(" ");
    }

    @Test
    void normalizeLeagueName_WithUnknownName_ReturnsOriginal() {
        String result = (String) ReflectionTestUtils.invokeMethod(adapter, "normalizeLeagueName", "Unknown League");
        assertThat(result).isEqualTo("Unknown League");
    }

    @Test
    void mapPosition_WithGoalkeeperText_ReturnsGK() throws Exception {
        String json = createSingleRowJson(createRowWithPosition("Goalkeeper", null));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("GK");
    }

    @Test
    void mapPosition_WithDefenderText_ReturnsDF() throws Exception {
        String json = createSingleRowJson(createRowWithPosition("Defender", null));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("DF");
    }

    @Test
    void mapPosition_WithMidfielderText_ReturnsMF() throws Exception {
        String json = createSingleRowJson(createRowWithPosition("Midfielder", null));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("MF");
    }

    @Test
    void mapPosition_WithForwardText_ReturnsFW() throws Exception {
        String json = createSingleRowJson(createRowWithPosition("Forward", null));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("FW");
    }

    @Test
    void mapPosition_WithStrikerText_ReturnsFW() throws Exception {
        String json = createSingleRowJson(createRowWithPosition("Striker", null));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("FW");
    }

    @Test
    void mapPosition_WithGKShortCode_ReturnsGK() throws Exception {
        String json = createSingleRowJson(createRowWithPosition(null, "GK"));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("GK");
    }

    @Test
    void mapPosition_WithDShortCode_ReturnsDF() throws Exception {
        String json = createSingleRowJson(createRowWithPosition(null, "D"));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("DF");
    }

    @Test
    void mapPosition_WithMShortCode_ReturnsMF() throws Exception {
        String json = createSingleRowJson(createRowWithPosition(null, "M"));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("MF");
    }

    @Test
    void mapPosition_WithFShortCode_ReturnsFW() throws Exception {
        String json = createSingleRowJson(createRowWithPosition(null, "F"));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("FW");
    }

    @Test
    void mapPosition_WithNoMatch_ReturnsMF() throws Exception {
        String json = createSingleRowJson(createRowWithPosition(null, null));

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result.get(0).getPosition()).isEqualTo("MF");
    }

    @Test
    void parsePlayerStats_WithNullFields_ReturnsNullForMissingValues() throws Exception {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("name", "Test");
        row.put("playerId", 1L);
        row.putNull("age");
        row.putNull("height");
        row.putNull("weight");
        row.putNull("rating");

        ArrayNode array = objectMapper.createArrayNode();
        array.add(row);
        String json = objectMapper.writeValueAsString(array);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAge()).isNull();
        assertThat(result.get(0).getHeight()).isNull();
        assertThat(result.get(0).getWeight()).isNull();
        assertThat(result.get(0).getRating()).isNull();
    }

    @Test
    void parsePlayerStats_WithInvalidNumericFields_ReturnsZero() throws Exception {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("name", "Test");
        row.put("playerId", 1L);
        row.put("age", "not-a-number");

        ArrayNode array = objectMapper.createArrayNode();
        array.add(row);
        String json = objectMapper.writeValueAsString(array);

        @SuppressWarnings("unchecked")
        List<PlayerStatsDTO> result = (List<PlayerStatsDTO>) ReflectionTestUtils.invokeMethod(
                adapter, "parsePlayerStats", json, "Premier League");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAge()).isZero();
    }

    private String createSingleRowJson(ObjectNode row) throws Exception {
        ArrayNode array = objectMapper.createArrayNode();
        array.add(row);
        return objectMapper.writeValueAsString(array);
    }

    private ObjectNode createMinimalPlayerNode(String name, long playerId) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        node.put("playerId", playerId);
        return node;
    }

    private ObjectNode createRowWithPosition(String positionText, String playedPositionsShort) {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("name", "Player");
        row.put("playerId", 1L);
        if (positionText != null) {
            row.put("positionText", positionText);
        } else {
            row.putNull("positionText");
        }
        if (playedPositionsShort != null) {
            row.put("playedPositionsShort", playedPositionsShort);
        } else {
            row.putNull("playedPositionsShort");
        }
        return row;
    }
}
