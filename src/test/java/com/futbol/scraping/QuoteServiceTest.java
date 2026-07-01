package com.futbol.scraping;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.dto.PlayerRankingDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerQuote;
import com.futbol.scraping.repository.PlayerQuoteRepository;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.strategy.ValuationStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@FutbolUnit
@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

        @Mock
        private PlayerRepository playerRepository;

        @Mock
        private PlayerQuoteRepository playerQuoteRepository;

        @Mock
        private ValuationStrategy performanceStrategy;

        @Mock
        private ValuationStrategy positionStrategy;

        private QuoteService quoteService;
        private Player testPlayer;
        private PlayerQuote testQuote;
        private SimpleMeterRegistry meterRegistry;

        @BeforeEach
        void setUp() {
                meterRegistry = new SimpleMeterRegistry();
                quoteService = new QuoteService(
                                playerRepository,
                                playerQuoteRepository,
                                performanceStrategy,
                                positionStrategy,
                                meterRegistry);

                testPlayer = Player.builder()
                                .id(1L)
                                .name("Cristiano Ronaldo")
                                .league("Serie A")
                                .team("Juventus")
                                .position("ST")
                                .build();

                testQuote = PlayerQuote.builder()
                                .id(1L)
                                .player(testPlayer)
                                .value(new BigDecimal("100.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("100.00"))
                                .build();
        }

        @Test
        void testRecalculate_Success() {
                // Arrange
                when(performanceStrategy.calculate(testPlayer)).thenReturn(new BigDecimal("110.00"));
                when(performanceStrategy.getVersion()).thenReturn("v1.0");
                when(playerRepository.findAll()).thenReturn(List.of(testPlayer));
                when(playerQuoteRepository.save(any(PlayerQuote.class))).thenReturn(testQuote);

                // Act
                RecalculateResponse response = quoteService.recalculate();

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("SUCCESS");
                assertThat(response.getPlayersProcessed()).isEqualTo(1);
                assertThat(response.getQuotesGenerated()).isEqualTo(1);
                assertThat(response.getStrategyUsed()).isEqualTo("v1.0");
                verify(playerRepository).findAll();
                verify(playerQuoteRepository).save(any(PlayerQuote.class));
        }

        @Test
        void testRecalculate_WithMultiplePlayers() {
                // Arrange
                Player player2 = Player.builder()
                                .id(2L)
                                .name("Lionel Messi")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("ST")
                                .build();

                when(performanceStrategy.calculate(any())).thenReturn(new BigDecimal("120.00"));
                when(performanceStrategy.getVersion()).thenReturn("v1.0");
                when(playerRepository.findAll()).thenReturn(List.of(testPlayer, player2));
                when(playerQuoteRepository.save(any(PlayerQuote.class))).thenReturn(testQuote);

                // Act
                RecalculateResponse response = quoteService.recalculate();

                // Assert
                assertThat(response.getPlayersProcessed()).isEqualTo(2);
                assertThat(response.getQuotesGenerated()).isEqualTo(2);
                verify(playerQuoteRepository, times(2)).save(any(PlayerQuote.class));
        }

        @Test
        void testGetPlayerQuotes_Success() {
                // Arrange
                PlayerQuote quote2 = PlayerQuote.builder()
                                .id(2L)
                                .player(testPlayer)
                                .value(new BigDecimal("105.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("105.00"))
                                .build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(List.of(testQuote, quote2));

                // Act
                List<QuoteDTO> quotes = quoteService.getPlayerQuotes(1L);

                // Assert
                assertThat(quotes).hasSize(2);
                assertThat(quotes.get(0).getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
                verify(playerRepository).findById(1L);
        }

        @Test
        void testGetCurrentQuote_Success() {
                // Arrange
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));

                // Act
                QuoteDTO quote = quoteService.getCurrentQuote(1L);

                // Assert
                assertThat(quote).isNotNull();
                assertThat(quote.getId()).isEqualTo(1L);
                assertThat(quote.getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        void testGetCurrentQuote_NoQuoteFound() {
                // Arrange
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> quoteService.getCurrentQuote(1L))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("No quote found");
        }

        @Test
        void testGetQuoteAtDate_Success() {
                // Arrange
                LocalDateTime targetDate = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findByPlayerAndDateBefore(testPlayer, targetDate))
                                .thenReturn(List.of(testQuote));

                // Act
                QuoteDTO quote = quoteService.getQuoteAtDate(1L, targetDate);

                // Assert
                assertThat(quote).isNotNull();
                assertThat(quote.getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        void testGetQuoteAtDate_NotFound() {
                // Arrange
                LocalDateTime targetDate = LocalDateTime.now(ZoneOffset.UTC).minusDays(10);
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findByPlayerAndDateBefore(testPlayer, targetDate))
                                .thenReturn(Collections.emptyList());

                // Act & Assert
                assertThatThrownBy(() -> quoteService.getQuoteAtDate(1L, targetDate))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void testGetRanking_Success() {
                Player player2 = Player.builder()
                                .id(2L)
                                .name("Lionel Messi")
                                .league("Ligue 1")
                                .team("PSG")
                                .position("ST")
                                .build();

                PlayerQuote messiQuote = PlayerQuote.builder()
                                .id(2L)
                                .player(player2)
                                .value(new BigDecimal("120.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("120.00"))
                                .build();

                when(playerRepository.findAll()).thenReturn(List.of(testPlayer, player2));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player2))
                                .thenReturn(Optional.of(messiQuote));

                List<PlayerRankingDTO> ranking = quoteService.getRanking();

                assertThat(ranking).hasSize(2);
                assertThat(ranking.get(0).getPlayerName()).isEqualTo("Lionel Messi");
                assertThat(ranking.get(0).getRank()).isEqualTo(1);
                assertThat(ranking.get(1).getRank()).isEqualTo(2);
        }

        @Test
        void testGetCurrentPrice_WithQuote() {
                // Arrange
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(testQuote));

                // Act
                BigDecimal price = quoteService.getCurrentPrice(testPlayer);

                // Assert
                assertThat(price).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        void testGetCurrentPrice_NoQuote() {
                // Arrange
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.empty());

                // Act
                BigDecimal price = quoteService.getCurrentPrice(testPlayer);

                // Assert
                assertThat(price).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        void testSetActiveStrategy_PerformanceBased() {
                // Act & Assert
                assertThatCode(() -> quoteService.setActiveStrategy("performanceBased"))
                                .doesNotThrowAnyException();
        }

        @Test
        void testSetActiveStrategy_PositionWeighted() {
                // Act & Assert
                assertThatCode(() -> quoteService.setActiveStrategy("positionWeighted"))
                                .doesNotThrowAnyException();
        }

        @Test
        void testSetActiveStrategy_Invalid() {
                // Act & Assert
                assertThatThrownBy(() -> quoteService.setActiveStrategy("invalidStrategy"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Unknown strategy");
        }

        @Test
        void testRecalculate_PlayerCalculationException() {
                // Arrange - one player throws exception, other succeeds
                Player player2 = Player.builder()
                                .id(2L)
                                .name("Player 2")
                                .league("La Liga")
                                .team("Barcelona")
                                .position("ST")
                                .build();

                when(performanceStrategy.calculate(testPlayer)).thenThrow(new RuntimeException("Calculation failed"));
                when(performanceStrategy.calculate(player2)).thenReturn(new BigDecimal("120.00"));
                when(performanceStrategy.getVersion()).thenReturn("v1.0");
                when(playerRepository.findAll()).thenReturn(List.of(testPlayer, player2));
                when(playerQuoteRepository.save(any(PlayerQuote.class))).thenReturn(testQuote);

                // Act
                RecalculateResponse response = quoteService.recalculate();

                // Assert - should continue with next player despite exception
                assertThat(response.getStatus()).isEqualTo("SUCCESS");
                assertThat(response.getPlayersProcessed()).isEqualTo(2);
                assertThat(response.getQuotesGenerated()).isEqualTo(1); // Only one succeeded
                verify(playerQuoteRepository, times(1)).save(any(PlayerQuote.class));
        }

        @Test
        void testGetPlayerQuotes_PlayerNotFound_ThrowsException() {
                // Arrange
                when(playerRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> quoteService.getPlayerQuotes(999L))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Player not found with id: 999");
        }

        @Test
        void testGetCurrentQuote_PlayerNotFound() {
                // Arrange
                when(playerRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> quoteService.getCurrentQuote(999L))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void testGetQuoteAtDate_PlayerNotFound() {
                // Arrange
                LocalDateTime date = LocalDateTime.now(ZoneOffset.UTC);
                when(playerRepository.findById(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> quoteService.getQuoteAtDate(999L, date))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void testGetRanking_EmptyPlayerList() {
                when(playerRepository.findAll()).thenReturn(Collections.emptyList());

                List<PlayerRankingDTO> ranking = quoteService.getRanking();

                assertThat(ranking).isEmpty();
        }

        @Test
        void testGetRanking_CorrectRankAssignment() {
                Player player1 = Player.builder().id(1L).name("Player 1").league("PL").team("T1").position("ST")
                                .build();
                Player player2 = Player.builder().id(2L).name("Player 2").league("PL").team("T2").position("ST")
                                .build();
                Player player3 = Player.builder().id(3L).name("Player 3").league("PL").team("T3").position("ST")
                                .build();

                PlayerQuote quote1 = PlayerQuote.builder().id(1L).player(player1)
                                .value(new BigDecimal("80.00")).quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0").baseScore(new BigDecimal("80.00")).build();
                PlayerQuote quote2 = PlayerQuote.builder().id(2L).player(player2)
                                .value(new BigDecimal("120.00")).quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0").baseScore(new BigDecimal("120.00")).build();
                PlayerQuote quote3 = PlayerQuote.builder().id(3L).player(player3)
                                .value(new BigDecimal("100.00")).quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0").baseScore(new BigDecimal("100.00")).build();

                when(playerRepository.findAll()).thenReturn(List.of(player1, player2, player3));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player1)).thenReturn(Optional.of(quote1));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player2)).thenReturn(Optional.of(quote2));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player3)).thenReturn(Optional.of(quote3));

                List<PlayerRankingDTO> ranking = quoteService.getRanking();

                assertThat(ranking).hasSize(3);
                assertThat(ranking.get(0).getPlayerName()).isEqualTo("Player 2");
                assertThat(ranking.get(0).getRank()).isEqualTo(1);
                assertThat(ranking.get(1).getPlayerName()).isEqualTo("Player 3");
                assertThat(ranking.get(1).getRank()).isEqualTo(2);
                assertThat(ranking.get(2).getPlayerName()).isEqualTo("Player 1");
                assertThat(ranking.get(2).getRank()).isEqualTo(3);
        }

        @Test
        void testGetCurrentPrice_NegativeValue() {
                // Arrange - quote with negative value (edge case)
                PlayerQuote negativeQuote = PlayerQuote.builder()
                                .id(1L)
                                .player(testPlayer)
                                .value(new BigDecimal("-50.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("-50.00"))
                                .build();

                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(negativeQuote));

                // Act
                BigDecimal price = quoteService.getCurrentPrice(testPlayer);

                // Assert
                assertThat(price).isEqualByComparingTo(new BigDecimal("-50.00"));
        }

        @Test
        void testGetCurrentPrice_ZeroValue() {
                // Arrange - quote with zero value
                PlayerQuote zeroQuote = PlayerQuote.builder()
                                .id(1L)
                                .player(testPlayer)
                                .value(BigDecimal.ZERO)
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0")
                                .baseScore(BigDecimal.ZERO)
                                .build();

                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(zeroQuote));

                // Act
                BigDecimal price = quoteService.getCurrentPrice(testPlayer);

                // Assert
                assertThat(price).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void testGetRanking_SameScorePlayersSorted() {
                Player player1 = Player.builder().id(1L).name("Player A").league("PL").team("T1").position("ST")
                                .build();
                Player player2 = Player.builder().id(2L).name("Player B").league("PL").team("T2").position("ST")
                                .build();

                PlayerQuote tiedQuote = PlayerQuote.builder()
                                .id(1L).player(player1)
                                .value(new BigDecimal("100.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("100.00"))
                                .build();

                when(playerRepository.findAll()).thenReturn(List.of(player1, player2));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(any())).thenReturn(Optional.of(tiedQuote));

                List<PlayerRankingDTO> ranking = quoteService.getRanking();

                assertThat(ranking).hasSize(2);
                assertThat(ranking.get(0).getScore()).isEqualByComparingTo(new BigDecimal("100.00"));
                assertThat(ranking.get(1).getScore()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        void testSetActiveStrategy_SwitchBetweenStrategies() {
                PlayerQuote perfQuote = PlayerQuote.builder()
                                .id(1L).player(testPlayer)
                                .value(new BigDecimal("100.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("perf-v1")
                                .baseScore(new BigDecimal("100.00"))
                                .build();
                PlayerQuote posQuote = PlayerQuote.builder()
                                .id(2L).player(testPlayer)
                                .value(new BigDecimal("120.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC))
                                .strategyVersion("pos-v1")
                                .baseScore(new BigDecimal("120.00"))
                                .build();

                when(playerRepository.findAll()).thenReturn(List.of(testPlayer));
                when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Optional.of(perfQuote))
                                .thenReturn(Optional.of(posQuote));

                quoteService.setActiveStrategy("performanceBased");
                List<PlayerRankingDTO> ranking1 = quoteService.getRanking();

                quoteService.setActiveStrategy("positionWeighted");
                List<PlayerRankingDTO> ranking2 = quoteService.getRanking();

                assertThat(ranking1.get(0).getStrategyVersion()).isEqualTo("perf-v1");
                assertThat(ranking2.get(0).getStrategyVersion()).isEqualTo("pos-v1");
        }

        @Test
        void testGetQuoteAtDate_MultipleQuotesReturnsFirst() {
                // Arrange - multiple quotes before date
                LocalDateTime targetDate = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
                PlayerQuote quote1 = PlayerQuote.builder()
                                .id(1L)
                                .player(testPlayer)
                                .value(new BigDecimal("110.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("110.00"))
                                .build();

                PlayerQuote quote2 = PlayerQuote.builder()
                                .id(2L)
                                .player(testPlayer)
                                .value(new BigDecimal("100.00"))
                                .quoteDate(LocalDateTime.now(ZoneOffset.UTC).minusDays(2))
                                .strategyVersion("v1.0")
                                .baseScore(new BigDecimal("100.00"))
                                .build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findByPlayerAndDateBefore(testPlayer, targetDate))
                                .thenReturn(List.of(quote1, quote2));

                // Act
                QuoteDTO quote = quoteService.getQuoteAtDate(1L, targetDate);

                // Assert - should return first (most recent) quote
                assertThat(quote.getId()).isEqualTo(1L);
                assertThat(quote.getValue()).isEqualByComparingTo(new BigDecimal("110.00"));
        }

        @Test
        void testRecalculate_EmptyPlayerList() {
                // Arrange
                when(playerRepository.findAll()).thenReturn(Collections.emptyList());
                when(performanceStrategy.getVersion()).thenReturn("v1.0");

                // Act
                RecalculateResponse response = quoteService.recalculate();

                // Assert
                assertThat(response.getPlayersProcessed()).isZero();
                assertThat(response.getQuotesGenerated()).isZero();
                assertThat(response.getStatus()).isEqualTo("SUCCESS");
                verify(playerQuoteRepository, never()).save(any());
        }

        @Test
        void testGetPlayerQuotes_EmptyQuoteList() {
                // Arrange
                when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
                when(playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(testPlayer))
                                .thenReturn(Collections.emptyList());

                // Act
                List<QuoteDTO> quotes = quoteService.getPlayerQuotes(1L);

                // Assert
                assertThat(quotes).isEmpty();
        }
}
