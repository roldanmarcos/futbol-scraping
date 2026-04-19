package com.futbol.scraping.service;

import com.futbol.scraping.adapter.WhoScoredAdapter;
import com.futbol.scraping.dto.PlayerStatsDTO;
import com.futbol.scraping.model.Player;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingService {

    private final WhoScoredAdapter whoScoredAdapter;
    private final PlayerService playerService;

    private static final List<String> LEAGUES = List.of(
            "Premier League", "Bundesliga", "La Liga", "Serie A", "Ligue 1");

    public int syncAllLeagues() {
        int total = 0;
        for (String league : LEAGUES) {
            total += syncLeague(league);
        }
        log.info("Total players synced: {}", total);
        return total;
    }

    public int syncLeague(String league) {
        log.info("Syncing league: {}", league);

        List<PlayerStatsDTO> players = whoScoredAdapter.scrapePlayersByLeague(league);

        if (players.isEmpty()) {
            log.warn("No results from WhoScored for {}", league);
        }

        int count = 0;
        for (PlayerStatsDTO dto : players) {
            try {
                Player player = Player.builder()
                        .name(dto.getName())
                        .league(dto.getLeague() != null ? dto.getLeague() : league)
                        .team(dto.getTeam() != null ? dto.getTeam() : "Unknown")
                        .position(dto.getPosition() != null ? dto.getPosition() : "MF")
                        .positionText(dto.getPositionText())
                        .playedPositions(dto.getPlayedPositions())
                        .playedPositionsShort(dto.getPlayedPositionsShort())
                        .teamRegionName(dto.getTeamRegionName())
                        .regionCode(dto.getRegionCode())
                        .age(dto.getAge())
                        .height(dto.getHeight())
                        .weight(dto.getWeight())
                        .appearances(dto.getAppearances() != null ? dto.getAppearances() : 0)
                        .subOn(dto.getSubOn())
                        .manOfTheMatch(dto.getManOfTheMatch())
                        .goals(dto.getGoals() != null ? dto.getGoals() : 0)
                        .assists(dto.getAssists() != null ? dto.getAssists() : 0)
                        .minutesPlayed(dto.getMinutesPlayed())
                        .isManOfTheMatch(dto.getIsManOfTheMatch())
                        .isActive(dto.getIsActive())
                        .isOpta(dto.getIsOpta())
                        .tournamentShortName(dto.getTournamentShortName())
                        .tournamentId(dto.getTournamentId())
                        .tournamentName(dto.getTournamentName())
                        .tournamentRegionId(dto.getTournamentRegionId())
                        .tournamentRegionCode(dto.getTournamentRegionCode())
                        .tournamentRegionName(dto.getTournamentRegionName())
                        .seasonId(dto.getSeasonId())
                        .seasonName(dto.getSeasonName())
                        .rating(dto.getRating())
                        .shotsPerGame(dto.getShotsPerGame())
                        .aerialWonPerGame(dto.getAerialWonPerGame())
                        .yellowCard(dto.getYellowCard())
                        .redCard(dto.getRedCard())
                        .passSuccess(dto.getPassSuccess())
                        .ranking(dto.getRanking())
                        .playerId(dto.getPlayerId())
                        .firstName(dto.getFirstName())
                        .lastName(dto.getLastName())
                        .teamId(dto.getTeamId())
                        .whoscoredId(dto.getWhoscoredId())
                        .url(dto.getUrl())
                        .lastScrapedAt(System.currentTimeMillis())
                        .build();
                playerService.saveOrUpdatePlayer(player);
                count++;
            } catch (Exception e) {
                log.error("Failed to save player {}: {}", dto.getName(), e.getMessage());
            }
        }

        log.info("Synced {} players for league {}", count, league);
        return count;
    }
}
