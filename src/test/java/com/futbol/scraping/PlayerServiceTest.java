package com.futbol.scraping;

import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.PlayerDetailDTO;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerQuote;
import com.futbol.scraping.repository.PlayerQuoteRepository;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

        @Mock
        private PlayerRepository playerRepository;

        @Mock
        private PlayerQuoteRepository playerQuoteRepository;

        @InjectMocks
        private PlayerService playerService;

        private Player testPlayer;
        private PlayerQuote testQuote;

        @BeforeEach
        void setUp() {
                testPlayer = Player.builder()
                                .id(1L)
                                .name("Lionel Messi")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("ST")
                                .age(34)
                                .weight(72)
                                .appearances(700)
                                .goals(800)
                                .assists(300)
                                .whoscoredId("12345")
                                .build();

                testQuote = PlayerQuote.builder()
                                .id(1L)
                                .player(testPlayer)
                                .value(new BigDecimal("150.00"))
                                .quoteDate(LocalDateTime.now())
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("150.00"))
                                .build();
        }

        @Test
        void testGetPlayers_NoFilters() {
                // Arrange
                Player player2 = Player.builder()
                                .id(2L)
                                .name("Neymar")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("LW")
                                .build();

                when(playerRepository.findAll(any(Specification.class))).thenReturn(List.of(testPlayer, player2));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player2))
                                .thenReturn(Optional.empty());

                // Act
                List<PlayerDTO> players = playerService.getPlayers(null, null, null);

                // Assert
                assertThat(players).hasSize(2);
                assertThat(players.get(0).getName()).isEqualTo("Lionel Messi");
                verify(playerRepository).findAll(any(Specification.class));
        }

        @Test
        void testGetPlayers_WithLeagueFilter() {
                // Arrange
                when(playerRepository.findAll(any(Specification.class)))
                                .thenReturn(List.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));

                // Act
                List<PlayerDTO> players = playerService.getPlayers("Ligue 1", null, null);

                // Assert
                assertThat(players).hasSize(1);
                assertThat(players.get(0).getLeague()).isEqualTo("Ligue 1");
        }

        @Test
        void testGetPlayers_WithMultipleFilters() {
                // Arrange
                when(playerRepository.findAll(any(Specification.class)))
                                .thenReturn(List.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));

                // Act
                List<PlayerDTO> players = playerService.getPlayers("Ligue 1", "PSG", "ST");

                // Assert
                assertThat(players).hasSize(1);
                assertThat(players.get(0).getPosition()).isEqualTo("ST");
        }

        @Test
        void testGetPlayers_Empty() {
                // Arrange
                when(playerRepository.findAll(any(Specification.class)))
                                .thenReturn(Collections.emptyList());

                // Act
                List<PlayerDTO> players = playerService.getPlayers("Unknown", null, null);

                // Assert
                assertThat(players).isEmpty();
        }

        @Test
        void testGetPlayerById_Success() {
                // Arrange
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));
                when(playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(List.of(testQuote));

                // Act
                PlayerDetailDTO playerDetail = playerService.getPlayerById(1L);

                // Assert
                assertThat(playerDetail).isNotNull();
                assertThat(playerDetail.getId()).isEqualTo(1L);
                assertThat(playerDetail.getName()).isEqualTo("Lionel Messi");
                assertThat(playerDetail.getCurrentQuote()).isEqualByComparingTo(new BigDecimal("150.00"));
                assertThat(playerDetail.getRecentQuotes()).hasSize(1);
                verify(playerRepository).findById(1L);
        }

        @Test
        void testGetPlayerById_NotFound() {
                // Arrange
                when(playerRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> playerService.getPlayerById(999L))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Player not found");
                verify(playerRepository).findById(999L);
        }

        @Test
        void testGetPlayerById_NoQuotes() {
                // Arrange
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.empty());
                when(playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Collections.emptyList());

                // Act
                PlayerDetailDTO playerDetail = playerService.getPlayerById(1L);

                // Assert
                assertThat(playerDetail).isNotNull();
                assertThat(playerDetail.getCurrentQuote()).isNull();
                assertThat(playerDetail.getRecentQuotes()).isEmpty();
        }

        @Test
        void testSavePlayer_Success() {
                // Arrange
                when(playerRepository.save(testPlayer)).thenReturn(testPlayer);

                // Act
                Player saved = playerService.savePlayer(testPlayer);

                // Assert
                assertThat(saved).isEqualTo(testPlayer);
                verify(playerRepository).save(testPlayer);
        }

        @Test
        void testSaveOrUpdatePlayer_NewPlayer() {
                // Arrange
                Player newPlayer = Player.builder()
                                .id(null)
                                .name("Kylian Mbappe")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("ST")
                                .whoscoredId("54321")
                                .build();

                when(playerRepository.findByWhoscoredId("54321")).thenReturn(Optional.empty());
                when(playerRepository.save(newPlayer)).thenReturn(newPlayer);

                // Act
                Player saved = playerService.saveOrUpdatePlayer(newPlayer);

                // Assert
                assertThat(saved).isEqualTo(newPlayer);
                verify(playerRepository).findByWhoscoredId("54321");
                verify(playerRepository).save(newPlayer);
        }

        @Test
        void testSaveOrUpdatePlayer_ExistingPlayer() {
                // Arrange
                Player existingPlayer = Player.builder()
                                .id(1L)
                                .name("Lionel Messi")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("ST")
                                .age(34)
                                .whoscoredId("12345")
                                .build();

                Player updateData = Player.builder()
                                .name("Lionel Messi Updated")
                                .league("Barcelona")
                                .team("Barcelona")
                                .position("RW")
                                .age(35)
                                .whoscoredId("12345")
                                .build();

                when(playerRepository.findByWhoscoredId("12345")).thenReturn(Optional.of(existingPlayer));
                when(playerRepository.save(existingPlayer)).thenReturn(existingPlayer);

                // Act
                Player saved = playerService.saveOrUpdatePlayer(updateData);

                // Assert
                assertThat(saved.getId()).isEqualTo(1L);
                assertThat(saved.getLeague()).isEqualTo("Barcelona");
                verify(playerRepository).findByWhoscoredId("12345");
                verify(playerRepository).save(existingPlayer);
        }

        @Test
        void testFindById_Success() {
                // Arrange
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));

                // Act
                Optional<Player> found = playerService.findById(1L);

                // Assert
                assertThat(found).isPresent();
                assertThat(found.get().getName()).isEqualTo("Lionel Messi");
                verify(playerRepository).findById(1L);
        }

        @Test
        void testFindById_NotFound() {
                // Arrange
                when(playerRepository.findById(999L)).thenReturn(Optional.empty());

                // Act
                Optional<Player> found = playerService.findById(999L);

                // Assert
                assertThat(found).isEmpty();
        }

        @Test
        void testGetPlayers_WithBlankFilters() {
                // Arrange - blank strings should be treated as no filter
                Player player2 = Player.builder()
                                .id(2L)
                                .name("Neymar")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("LW")
                                .build();

                when(playerRepository.findAll(any(Specification.class)))
                                .thenReturn(List.of(testPlayer, player2));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(any()))
                                .thenReturn(Optional.of(testQuote));

                // Act
                List<PlayerDTO> players = playerService.getPlayers("  ", "  ", "  ");

                // Assert
                assertThat(players).hasSize(2);
                verify(playerRepository).findAll(any(Specification.class));
        }

        @Test
        void testGetPlayerById_WithRecentQuotesLimit() {
                // Arrange - should return max 10 recent quotes
                List<PlayerQuote> manyQuotes = new ArrayList<>();
                for (int i = 0; i < 15; i++) {
                        manyQuotes.add(PlayerQuote.builder()
                                        .id((long) i)
                                        .player(testPlayer)
                                        .value(new BigDecimal("100.00"))
                                        .quoteDate(LocalDateTime.now().minusDays(i))
                                        .strategyVersion("v1.0")
                                        .baseScore(new BigDecimal("100.00"))
                                        .build());
                }

                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));
                when(playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(manyQuotes);

                // Act
                PlayerDetailDTO playerDetail = playerService.getPlayerById(1L);

                // Assert
                assertThat(playerDetail.getRecentQuotes()).hasSize(10);
                verify(playerQuoteRepository).findByPlayerOrderByQuoteDateDesc(testPlayer);
        }

        @Test
        void testGetPlayerById_AllFieldsPopulated() {
                // Arrange - test with all fields filled
                Player completePlayer = Player.builder()
                                .id(1L)
                                .name("Complete Player")
                                .league("Premier League")
                                .team("Manchester United")
                                .position("CB")
                                .age(28)
                                .weight(85)
                                .height(190)
                                .appearances(250)
                                .goals(5)
                                .assists(3)
                                .whoscoredId("full-id")
                                .url("https://example.com")
                                .build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(completePlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(completePlayer))
                                .thenReturn(Optional.of(testQuote));
                when(playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(completePlayer))
                                .thenReturn(List.of(testQuote));

                // Act
                PlayerDetailDTO detail = playerService.getPlayerById(1L);

                // Assert
                assertThat(detail).isNotNull();
                assertThat(detail.getAge()).isEqualTo(28);
                assertThat(detail.getWeight()).isEqualTo(85);
                // assertThat(detail.getHeight()).isEqualTo(190);
                assertThat(detail.getAppearances()).isEqualTo(250);
                assertThat(detail.getGoals()).isEqualTo(5);
                assertThat(detail.getAssists()).isEqualTo(3);
        }

        @Test
        void testSaveOrUpdatePlayer_WithoutWhoscoredId() {
                // Arrange - player without whoscoredId should be saved without update check
                Player playerNoId = Player.builder()
                                .name("Player Without ID")
                                .league("Serie A")
                                .team("Juventus")
                                .position("ST")
                                .build();

                when(playerRepository.save(playerNoId)).thenReturn(playerNoId);

                // Act
                Player saved = playerService.saveOrUpdatePlayer(playerNoId);

                // Assert
                assertThat(saved).isEqualTo(playerNoId);
                verify(playerRepository, never()).findByWhoscoredId(any());
                verify(playerRepository).save(playerNoId);
        }

        @Test
        void testSaveOrUpdatePlayer_PartialUpdate() {
                // Arrange - test that only non-null fields are updated
                Player existingPlayer = Player.builder()
                                .id(1L)
                                .name("Existing")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("ST")
                                .age(30)
                                .goals(50)
                                .assists(20)
                                .whoscoredId("12345")
                                .build();

                Player updateData = Player.builder()
                                .name("Updated Name")
                                .league("Bundesliga")
                                .team("Bayern")
                                .position("RW")
                                .whoscoredId("12345")
                                // All other fields null - should not update them
                                .build();

                when(playerRepository.findByWhoscoredId("12345")).thenReturn(Optional.of(existingPlayer));
                when(playerRepository.save(existingPlayer)).thenReturn(existingPlayer);

                // Act
                playerService.saveOrUpdatePlayer(updateData);

                // Assert - verify that only provided fields were updated
                assertThat(existingPlayer.getName()).isEqualTo("Updated Name");
                assertThat(existingPlayer.getLeague()).isEqualTo("Bundesliga");
                assertThat(existingPlayer.getAge()).isEqualTo(30); // Should remain unchanged
                verify(playerRepository).save(existingPlayer);
        }

        @Test
        void testSaveOrUpdatePlayer_AllFieldsUpdated() {
                // Arrange - test that ALL optional fields are updated when provided
                Player existingPlayer = Player.builder()
                                .id(1L)
                                .name("Original")
                                .league("Serie A")
                                .team("Juventus")
                                .position("ST")
                                .age(25)
                                .weight(80)
                                .height(185)
                                .appearances(100)
                                .goals(20)
                                .assists(10)
                                .whoscoredId("12345")
                                .build();

                Player updateData = Player.builder()
                                .name("Updated Name")
                                .league("Premier League")
                                .team("Manchester United")
                                .position("CF")
                                .age(26)
                                .weight(82)
                                .height(187)
                                .appearances(110)
                                .goals(25)
                                .assists(12)
                                .positionText("Center Forward")
                                .playedPositions("CF,ST")
                                .playedPositionsShort("CF,ST")
                                .teamRegionName("England")
                                .regionCode("EN")
                                .subOn(5)
                                .manOfTheMatch(3)
                                .minutesPlayed(9000)
                                .isManOfTheMatch(true)
                                .isActive(true)
                                .isOpta(true)
                                .tournamentShortName("PL")
                                .tournamentId(1L)
                                .tournamentName("Premier League")
                                .tournamentRegionId(2L)
                                .tournamentRegionCode("ENG")
                                .tournamentRegionName("England")
                                .seasonId(3L)
                                .seasonName("2023/2024")
                                .rating(8.5)
                                .shotsPerGame(3.2)
                                .aerialWonPerGame(2.1)
                                .yellowCard(2D)
                                .redCard(0D)
                                .passSuccess(85.5)
                                .ranking(1)
                                .playerId(123L)
                                .firstName("John")
                                .lastName("Doe")
                                .teamId(456L)
                                .url("https://example.com/player")
                                .whoscoredId("12345")
                                .build();

                when(playerRepository.findByWhoscoredId("12345")).thenReturn(Optional.of(existingPlayer));
                when(playerRepository.save(existingPlayer)).thenReturn(existingPlayer);

                // Act
                playerService.saveOrUpdatePlayer(updateData);

                // Assert - verify ALL fields are updated
                assertThat(existingPlayer.getName()).isEqualTo("Updated Name");
                assertThat(existingPlayer.getLeague()).isEqualTo("Premier League");
                assertThat(existingPlayer.getTeam()).isEqualTo("Manchester United");
                assertThat(existingPlayer.getPosition()).isEqualTo("CF");
                assertThat(existingPlayer.getAge()).isEqualTo(26);
                assertThat(existingPlayer.getWeight()).isEqualTo(82);
                assertThat(existingPlayer.getHeight()).isEqualTo(187);
                assertThat(existingPlayer.getAppearances()).isEqualTo(110);
                assertThat(existingPlayer.getGoals()).isEqualTo(25);
                assertThat(existingPlayer.getAssists()).isEqualTo(12);
                assertThat(existingPlayer.getPositionText()).isEqualTo("Center Forward");
                assertThat(existingPlayer.getPlayedPositions()).isEqualTo("CF,ST");
                assertThat(existingPlayer.getPlayedPositionsShort()).isEqualTo("CF,ST");
                assertThat(existingPlayer.getTeamRegionName()).isEqualTo("England");
                assertThat(existingPlayer.getRegionCode()).isEqualTo("EN");
                assertThat(existingPlayer.getSubOn()).isEqualTo(5);
                assertThat(existingPlayer.getManOfTheMatch()).isEqualTo(3);
                assertThat(existingPlayer.getMinutesPlayed()).isEqualTo(9000);
                assertThat(existingPlayer.getIsManOfTheMatch()).isTrue();
                assertThat(existingPlayer.getIsActive()).isTrue();
                assertThat(existingPlayer.getIsOpta()).isTrue();
                assertThat(existingPlayer.getTournamentShortName()).isEqualTo("PL");
                assertThat(existingPlayer.getTournamentId()).isEqualTo(1L);
                assertThat(existingPlayer.getTournamentName()).isEqualTo("Premier League");
                assertThat(existingPlayer.getTournamentRegionId()).isEqualTo(2L);
                assertThat(existingPlayer.getTournamentRegionCode()).isEqualTo("ENG");
                assertThat(existingPlayer.getTournamentRegionName()).isEqualTo("England");
                assertThat(existingPlayer.getSeasonId()).isEqualTo(3L);
                assertThat(existingPlayer.getSeasonName()).isEqualTo("2023/2024");
                assertThat(existingPlayer.getRating()).isEqualTo(8.5);
                assertThat(existingPlayer.getShotsPerGame()).isEqualTo(3.2);
                assertThat(existingPlayer.getAerialWonPerGame()).isEqualTo(2.1);
                assertThat(existingPlayer.getYellowCard()).isEqualTo(2);
                assertThat(existingPlayer.getRedCard()).isZero();
                assertThat(existingPlayer.getPassSuccess()).isEqualTo(85.5);
                assertThat(existingPlayer.getRanking()).isEqualTo(1);
                assertThat(existingPlayer.getPlayerId()).isEqualTo(123L);
                assertThat(existingPlayer.getFirstName()).isEqualTo("John");
                assertThat(existingPlayer.getLastName()).isEqualTo("Doe");
                assertThat(existingPlayer.getTeamId()).isEqualTo(456L);
                assertThat(existingPlayer.getUrl()).isEqualTo("https://example.com/player");
                assertThat(existingPlayer.getLastScrapedAt()).isGreaterThan(0);

                verify(playerRepository).findByWhoscoredId("12345");
                verify(playerRepository).save(existingPlayer);
        }

        @Test
        void testGetPlayers_CaseInsensitiveFiltering() {
                // Arrange - filters should be case-insensitive
                when(playerRepository.findAll(any(Specification.class)))
                                .thenReturn(List.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));

                // Act
                List<PlayerDTO> result = playerService.getPlayers("ligue 1", "psg", "st");

                // Assert
                assertThat(result).hasSize(1);
                verify(playerRepository).findAll(any(Specification.class));
        }

        @Test
        void testSaveOrUpdatePlayer_LastScrapedAtUpdated() {
                // Arrange
                Player existingPlayer = Player.builder()
                                .id(1L)
                                .name("Player")
                                .whoscoredId("12345")
                                .lastScrapedAt(1000L)
                                .build();

                Player updateData = Player.builder()
                                .name("Player Updated")
                                .whoscoredId("12345")
                                .build();

                when(playerRepository.findByWhoscoredId("12345")).thenReturn(Optional.of(existingPlayer));
                when(playerRepository.save(existingPlayer)).thenReturn(existingPlayer);

                // Act
                playerService.saveOrUpdatePlayer(updateData);

                // Assert - lastScrapedAt should be updated
                assertThat(existingPlayer.getLastScrapedAt()).isGreaterThan(1000L);
        }

        @Test
        void testGetPlayerById_PlayerNotFound_NeverCallsQuoteRepository() {
                // Arrange
                when(playerRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> playerService.getPlayerById(999L))
                                .isInstanceOf(ResourceNotFoundException.class);

                // Verify that quote repository was never called
                verify(playerQuoteRepository, never()).findTopByPlayerOrderByQuoteDateDesc(any());
                verify(playerQuoteRepository, never()).findByPlayerOrderByQuoteDateDesc(any());
        }

        @Test
        void testGetPlayers_TeamFilterWildcard() {
                // Arrange - team filter uses LIKE with wildcards
                when(playerRepository.findAll(any(Specification.class)))
                                .thenReturn(List.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));

                // Act - "PSG" should match "PSG", "PSG_A", "A_PSG", etc.
                List<PlayerDTO> result = playerService.getPlayers(null, "PSG", null);

                // Assert
                assertThat(result).hasSize(1);
        }
}
