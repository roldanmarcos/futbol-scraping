package com.futbol.scraping.service;

import com.futbol.scraping.adapter.FootballDataAdapter;
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

    private final FootballDataAdapter footballDataAdapter;
    private final WhoScoredAdapter whoScoredAdapter;
    private final PlayerService playerService;

    private static final List<String> LEAGUES = List.of(
            "Premier League", "Bundesliga", "La Liga", "Serie A", "Ligue 1"
    );

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

        List<PlayerStatsDTO> players = footballDataAdapter.fetchPlayersByLeague(league);

        if (players.isEmpty()) {
            log.info("No results from Football-Data.org for {}, trying WhoScored...", league);
            players = whoScoredAdapter.scrapePlayersByLeague(league);
        }

        if (players.isEmpty()) {
            log.info("No results from external sources for {}, using fallback data", league);
            players = getFallbackPlayers(league);
        }

        int count = 0;
        for (PlayerStatsDTO dto : players) {
            try {
                Player player = Player.builder()
                        .name(dto.getName())
                        .league(league)
                        .team(dto.getTeam() != null ? dto.getTeam() : "Unknown")
                        .position(dto.getPosition() != null ? dto.getPosition() : "MF")
                        .nationality(dto.getNationality())
                        .age(dto.getAge())
                        .weight(dto.getWeight())
                        .appearances(dto.getAppearances() != null ? dto.getAppearances() : 0)
                        .goals(dto.getGoals() != null ? dto.getGoals() : 0)
                        .assists(dto.getAssists() != null ? dto.getAssists() : 0)
                        .whoscoredId(dto.getWhoscoredId())
                        .url(dto.getUrl())
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

    /**
     * Fallback data for demonstration when external APIs are unavailable.
     */
    private List<PlayerStatsDTO> getFallbackPlayers(String league) {
        return switch (league) {
            case "Premier League" -> List.of(
                PlayerStatsDTO.builder().whoscoredId("ws-83532").name("Harry Kane").team("Tottenham Hotspur").league(league).position("FW").nationality("England").age(31).appearances(28).goals(22).assists(8).url("https://www.whoscored.com/players/83532/matchstatistics/harry-kane").build(),
                PlayerStatsDTO.builder().whoscoredId("ws-pl-2").name("Mohamed Salah").team("Liverpool").league(league).position("FW").nationality("Egypt").age(32).appearances(30).goals(18).assists(10).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-pl-3").name("Erling Haaland").team("Manchester City").league(league).position("FW").nationality("Norway").age(24).appearances(27).goals(20).assists(4).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-pl-4").name("Bukayo Saka").team("Arsenal").league(league).position("MF").nationality("England").age(23).appearances(30).goals(12).assists(11).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-pl-5").name("Cole Palmer").team("Chelsea").league(league).position("MF").nationality("England").age(22).appearances(29).goals(16).assists(9).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-pl-6").name("Kevin De Bruyne").team("Manchester City").league(league).position("MF").nationality("Belgium").age(33).appearances(18).goals(4).assists(10).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-pl-7").name("Virgil van Dijk").team("Liverpool").league(league).position("DF").nationality("Netherlands").age(33).appearances(31).goals(3).assists(2).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-pl-8").name("Trent Alexander-Arnold").team("Liverpool").league(league).position("DF").nationality("England").age(26).appearances(28).goals(2).assists(8).build()
            );
            case "Bundesliga" -> List.of(
                PlayerStatsDTO.builder().whoscoredId("ws-bun-1").name("Florian Wirtz").team("Bayer Leverkusen").league(league).position("MF").nationality("Germany").age(21).appearances(30).goals(14).assists(12).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-bun-2").name("Jamal Musiala").team("Bayern Munich").league(league).position("MF").nationality("Germany").age(21).appearances(28).goals(12).assists(9).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-bun-3").name("Serhou Guirassy").team("Borussia Dortmund").league(league).position("FW").nationality("Guinea").age(28).appearances(25).goals(17).assists(3).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-bun-4").name("Viktor Boniface").team("Bayer Leverkusen").league(league).position("FW").nationality("Nigeria").age(24).appearances(22).goals(14).assists(6).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-bun-5").name("Joshua Kimmich").team("Bayern Munich").league(league).position("MF").nationality("Germany").age(29).appearances(30).goals(3).assists(11).build()
            );
            case "La Liga" -> List.of(
                PlayerStatsDTO.builder().whoscoredId("ws-lla-1").name("Vinicius Jr").team("Real Madrid").league(league).position("FW").nationality("Brazil").age(24).appearances(30).goals(17).assists(8).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-lla-2").name("Jude Bellingham").team("Real Madrid").league(league).position("MF").nationality("England").age(21).appearances(28).goals(13).assists(7).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-lla-3").name("Robert Lewandowski").team("Barcelona").league(league).position("FW").nationality("Poland").age(36).appearances(29).goals(19).assists(7).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-lla-4").name("Lamine Yamal").team("Barcelona").league(league).position("FW").nationality("Spain").age(17).appearances(30).goals(8).assists(13).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-lla-5").name("Antoine Griezmann").team("Atletico Madrid").league(league).position("FW").nationality("France").age(33).appearances(29).goals(14).assists(8).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-lla-6").name("Pedri").team("Barcelona").league(league).position("MF").nationality("Spain").age(22).appearances(24).goals(5).assists(8).build()
            );
            case "Serie A" -> List.of(
                PlayerStatsDTO.builder().whoscoredId("ws-sa-1").name("Lautaro Martinez").team("Inter Milan").league(league).position("FW").nationality("Argentina").age(27).appearances(30).goals(20).assists(7).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-sa-2").name("Khvicha Kvaratskhelia").team("Napoli").league(league).position("FW").nationality("Georgia").age(23).appearances(27).goals(11).assists(9).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-sa-3").name("Marcus Thuram").team("Inter Milan").league(league).position("FW").nationality("France").age(27).appearances(29).goals(14).assists(10).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-sa-4").name("Dusan Vlahovic").team("Juventus").league(league).position("FW").nationality("Serbia").age(24).appearances(26).goals(15).assists(3).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-sa-5").name("Nicolo Barella").team("Inter Milan").league(league).position("MF").nationality("Italy").age(27).appearances(30).goals(5).assists(9).build()
            );
            case "Ligue 1" -> List.of(
                PlayerStatsDTO.builder().whoscoredId("ws-300713").name("Kylian Mbappé").team("Real Madrid").league(league).position("FW").nationality("France").age(26).appearances(29).goals(27).assists(7).url("https://www.whoscored.com/players/300713/matchstatistics/kylian-mbappé").build(),
                PlayerStatsDTO.builder().whoscoredId("ws-l1-2").name("Alexandre Lacazette").team("Lyon").league(league).position("FW").nationality("France").age(33).appearances(28).goals(17).assists(5).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-234364").name("Ludovic Ajorque").team("Strasbourg").league(league).position("FW").nationality("France").age(30).appearances(26).goals(12).assists(4).url("https://www.whoscored.com/players/234364/matchstatistics/ludovic-ajorque").build(),
                PlayerStatsDTO.builder().whoscoredId("ws-l1-4").name("Jonathan David").team("Lille").league(league).position("FW").nationality("Canada").age(24).appearances(29).goals(20).assists(6).build(),
                PlayerStatsDTO.builder().whoscoredId("ws-l1-5").name("Amine Gouiri").team("Rennes").league(league).position("FW").nationality("France").age(24).appearances(28).goals(12).assists(7).build()
            );
            default -> List.of();
        };
    }
}
