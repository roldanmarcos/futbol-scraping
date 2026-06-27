package com.futbol.scraping;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.adapter.WhoScoredAdapter;
import com.futbol.scraping.dto.PlayerStatsDTO;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.service.PlayerService;
import com.futbol.scraping.service.ScrapingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@FutbolUnit
@ExtendWith(MockitoExtension.class)
class ScrapingServiceTest {

        @Mock
        private WhoScoredAdapter whoScoredAdapter;

        @Mock
        private PlayerService playerService;

        @InjectMocks
        private ScrapingService scrapingService;

        private PlayerStatsDTO testPlayerDto;

        @BeforeEach
        void setUp() {
                testPlayerDto = PlayerStatsDTO.builder()
                                .name("Cristiano Ronaldo")
                                .league("Serie A")
                                .team("Juventus")
                                .position("ST")
                                .positionText("Striker")
                                .playedPositions("CF,ST")
                                .playedPositionsShort("CF,ST")
                                .teamRegionName("Italy")
                                .regionCode("IT")
                                .age(36)
                                .height(187)
                                .weight(84)
                                .appearances(700)
                                .subOn(10)
                                .manOfTheMatch(50)
                                .goals(800)
                                .assists(300)
                                .minutesPlayed(50000)
                                .isManOfTheMatch(true)
                                .isActive(true)
                                .isOpta(true)
                                .tournamentShortName("Serie A")
                                .tournamentId(1L)
                                .tournamentName("Serie A")
                                .tournamentRegionId(2L)
                                .tournamentRegionCode("IT")
                                .tournamentRegionName("Italy")
                                .seasonId(3L)
                                .seasonName("2021/2022")
                                .rating(8.5)
                                .shotsPerGame(3.5)
                                .aerialWonPerGame(2.1)
                                .yellowCard(5D)
                                .redCard(0D)
                                .passSuccess(85.5)
                                .ranking(1)
                                .playerId(12345L)
                                .firstName("Cristiano")
                                .lastName("Ronaldo")
                                .teamId(67890L)
                                .whoscoredId("54321")
                                .url("https://example.com/player/54321")
                                .build();
        }

        @Test
        void testSyncLeague_Success() {
                // Arrange
                PlayerStatsDTO player2 = PlayerStatsDTO.builder()
                                .name("Neymar")
                                .league("Serie A")
                                .team("Juventus")
                                .position("LW")
                                .goals(50)
                                .assists(100)
                                .appearances(300)
                                .build();

                when(whoScoredAdapter.scrapePlayersByLeague("Serie A"))
                                .thenReturn(List.of(testPlayerDto, player2));
                when(playerService.saveOrUpdatePlayer(any(Player.class)))
                                .thenAnswer(invocation -> {
                                        Player p = invocation.getArgument(0);
                                        p.setId(1L);
                                        return p;
                                });

                // Act
                int result = scrapingService.syncLeague("Serie A");

                // Assert
                assertThat(result).isEqualTo(2);
                verify(whoScoredAdapter).scrapePlayersByLeague("Serie A");
                verify(playerService, times(2)).saveOrUpdatePlayer(any(Player.class));
        }

        @Test
        void testSyncLeague_EmptyResults() {
                // Arrange
                when(whoScoredAdapter.scrapePlayersByLeague("Unknown League"))
                                .thenReturn(Collections.emptyList());

                // Act
                int result = scrapingService.syncLeague("Unknown League");

                // Assert
                assertThat(result).isZero();
                verify(whoScoredAdapter).scrapePlayersByLeague("Unknown League");
                verify(playerService, never()).saveOrUpdatePlayer(any(Player.class));
        }

        @Test
        void testSyncLeague_WithException() {
                // Arrange
                PlayerStatsDTO validPlayer = PlayerStatsDTO.builder()
                                .name("Valid Player")
                                .league("Serie A")
                                .team("Team")
                                .position("ST")
                                .build();

                PlayerStatsDTO problematicPlayer = PlayerStatsDTO.builder()
                                .name("Problematic Player")
                                .league("Serie A")
                                .team("Team")
                                .position("ST")
                                .build();

                when(whoScoredAdapter.scrapePlayersByLeague("Serie A"))
                                .thenReturn(List.of(validPlayer, problematicPlayer));
                when(playerService.saveOrUpdatePlayer(any(Player.class)))
                                .thenAnswer(invocation -> {
                                        Player p = invocation.getArgument(0);
                                        if (p.getName().equals("Problematic Player")) {
                                                throw new RuntimeException("Save failed");
                                        }
                                        p.setId(1L);
                                        return p;
                                });

                // Act
                int result = scrapingService.syncLeague("Serie A");

                // Assert - should save the valid one and skip the problematic one
                assertThat(result).isEqualTo(1);
                verify(playerService, times(2)).saveOrUpdatePlayer(any(Player.class));
        }

        @Test
        void testSyncAllLeagues_Success() {
                // Arrange
                when(whoScoredAdapter.scrapePlayersByLeague(anyString()))
                                .thenReturn(List.of(testPlayerDto));
                when(playerService.saveOrUpdatePlayer(any(Player.class)))
                                .thenAnswer(invocation -> {
                                        Player p = invocation.getArgument(0);
                                        p.setId(1L);
                                        return p;
                                });

                // Act
                int result = scrapingService.syncAllLeagues();

                // Assert - 5 leagues * 1 player each = 5
                assertThat(result).isEqualTo(5);
                verify(whoScoredAdapter, times(5)).scrapePlayersByLeague(anyString());
                verify(playerService, times(5)).saveOrUpdatePlayer(any(Player.class));
        }

        @Test
        void testSyncAllLeagues_MixedResults() {
                // Arrange
                when(whoScoredAdapter.scrapePlayersByLeague("Premier League"))
                                .thenReturn(List.of(testPlayerDto));
                when(whoScoredAdapter.scrapePlayersByLeague("Bundesliga"))
                                .thenReturn(Collections.emptyList());
                when(whoScoredAdapter.scrapePlayersByLeague("La Liga"))
                                .thenReturn(List.of(testPlayerDto, testPlayerDto));
                when(whoScoredAdapter.scrapePlayersByLeague("Serie A"))
                                .thenReturn(List.of(testPlayerDto));
                when(whoScoredAdapter.scrapePlayersByLeague("Ligue 1"))
                                .thenReturn(Collections.emptyList());
                when(playerService.saveOrUpdatePlayer(any(Player.class)))
                                .thenAnswer(invocation -> {
                                        Player p = invocation.getArgument(0);
                                        p.setId(1L);
                                        return p;
                                });

                // Act
                int result = scrapingService.syncAllLeagues();

                // Assert - 1 + 0 + 2 + 1 + 0 = 4
                assertThat(result).isEqualTo(4);
                verify(whoScoredAdapter, times(5)).scrapePlayersByLeague(anyString());
                verify(playerService, times(4)).saveOrUpdatePlayer(any(Player.class));
        }

        @Test
        void testSyncLeague_MapsPlayerDataCorrectly() {
                // Arrange
                when(whoScoredAdapter.scrapePlayersByLeague("Serie A"))
                                .thenReturn(List.of(testPlayerDto));
                when(playerService.saveOrUpdatePlayer(any(Player.class)))
                                .thenAnswer(invocation -> {
                                        Player p = invocation.getArgument(0);
                                        p.setId(1L);
                                        return p;
                                });

                // Act
                scrapingService.syncLeague("Serie A");

                // Assert - verify the player data was correctly mapped
                verify(playerService)
                                .saveOrUpdatePlayer(argThat(player -> player.getName().equals("Cristiano Ronaldo") &&
                                                player.getLeague().equals("Serie A") &&
                                                player.getTeam().equals("Juventus") &&
                                                player.getPosition().equals("ST") &&
                                                player.getAge() == 36 &&
                                                player.getGoals() == 800 &&
                                                player.getAssists() == 300));
        }
}
