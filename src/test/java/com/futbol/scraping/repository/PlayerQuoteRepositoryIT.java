package com.futbol.scraping.repository;

import com.futbol.scraping.annotation.FutbolJpaIT;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolJpaIT
class PlayerQuoteRepositoryIT {

    @Autowired
    private PlayerQuoteRepository playerQuoteRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private Player messi;
    private Player ronaldo;

    private LocalDateTime day1;
    private LocalDateTime day2;
    private LocalDateTime day3;

    @BeforeEach
    void setUp() {
        playerQuoteRepository.deleteAll();
        playerRepository.deleteAll();

        messi = playerRepository.save(Player.builder().name("Messi").league("MLS").team("Inter Miami").position("FW").build());
        ronaldo = playerRepository.save(Player.builder().name("Ronaldo").league("Saudi Pro").team("Al Nassr").position("FW").build());

        day1 = LocalDateTime.of(2024, 1, 1, 12, 0);
        day2 = LocalDateTime.of(2024, 2, 1, 12, 0);
        day3 = LocalDateTime.of(2024, 3, 1, 12, 0);

        playerQuoteRepository.save(PlayerQuote.builder().player(messi).value(new BigDecimal("100.00")).quoteDate(day1).strategyVersion("v1").build());
        playerQuoteRepository.save(PlayerQuote.builder().player(messi).value(new BigDecimal("120.00")).quoteDate(day2).strategyVersion("v1").build());
        playerQuoteRepository.save(PlayerQuote.builder().player(messi).value(new BigDecimal("150.00")).quoteDate(day3).strategyVersion("v1").build());
        playerQuoteRepository.save(PlayerQuote.builder().player(ronaldo).value(new BigDecimal("90.00")).quoteDate(day2).strategyVersion("v1").build());
    }

    @Test
    void findByPlayerId_returnsQuotesOrderedByDateDesc() {
        List<PlayerQuote> quotes = playerQuoteRepository.findByPlayerId(messi.getId());

        assertThat(quotes).hasSize(3);
        assertThat(quotes.get(0).getQuoteDate()).isEqualTo(day3);
        assertThat(quotes.get(2).getQuoteDate()).isEqualTo(day1);
    }

    @Test
    void findByPlayerId_withPlayerWithNoQuotes_returnsEmpty() {
        Player newPlayer = playerRepository.save(Player.builder().name("Neymar").league("MLS").team("Santos").position("FW").build());

        assertThat(playerQuoteRepository.findByPlayerId(newPlayer.getId())).isEmpty();
    }

    @Test
    void findByPlayerOrderByQuoteDateDesc_returnsAllQuotesNewestFirst() {
        List<PlayerQuote> quotes = playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(messi);

        assertThat(quotes).hasSize(3);
        assertThat(quotes.get(0).getValue()).isEqualByComparingTo("150.00");
        assertThat(quotes.get(1).getValue()).isEqualByComparingTo("120.00");
        assertThat(quotes.get(2).getValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void findTopByPlayerOrderByQuoteDateDesc_returnsMostRecentQuote() {
        Optional<PlayerQuote> top = playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(messi);

        assertThat(top).isPresent();
        assertThat(top.get().getValue()).isEqualByComparingTo("150.00");
        assertThat(top.get().getQuoteDate()).isEqualTo(day3);
    }

    @Test
    void findTopByPlayerOrderByQuoteDateDesc_withNoQuotes_returnsEmpty() {
        Player newPlayer = playerRepository.save(Player.builder().name("Neymar").league("MLS").team("Santos").position("FW").build());

        assertThat(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(newPlayer)).isEmpty();
    }

    @Test
    void findByPlayerAndDateBefore_returnsQuotesUpToDate() {
        // día de corte: 1 Feb 23:59 → debería devolver day1 y day2, no day3
        LocalDateTime cutoff = LocalDateTime.of(2024, 2, 1, 23, 59);
        List<PlayerQuote> quotes = playerQuoteRepository.findByPlayerAndDateBefore(messi, cutoff);

        assertThat(quotes).hasSize(2);
        assertThat(quotes).extracting(PlayerQuote::getValue)
                .containsExactlyInAnyOrder(new BigDecimal("100.00"), new BigDecimal("120.00"));
    }

    @Test
    void findByPlayerAndDateBefore_withCutoffBeforeAllQuotes_returnsEmpty() {
        LocalDateTime cutoff = LocalDateTime.of(2023, 12, 31, 23, 59);

        assertThat(playerQuoteRepository.findByPlayerAndDateBefore(messi, cutoff)).isEmpty();
    }
}
