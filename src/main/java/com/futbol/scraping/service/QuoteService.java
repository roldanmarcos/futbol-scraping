package com.futbol.scraping.service;

import com.futbol.scraping.dto.PlayerRankingDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerQuote;
import com.futbol.scraping.repository.PlayerQuoteRepository;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.strategy.ValuationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuoteService {

    private final PlayerRepository playerRepository;
    private final PlayerQuoteRepository playerQuoteRepository;
    private final ValuationStrategy performanceStrategy;
    private final ValuationStrategy positionStrategy;

    private String activeStrategy = "performanceBased";

    public QuoteService(
            PlayerRepository playerRepository,
            PlayerQuoteRepository playerQuoteRepository,
            @Qualifier("performanceBased") ValuationStrategy performanceStrategy,
            @Qualifier("positionWeighted") ValuationStrategy positionStrategy) {
        this.playerRepository = playerRepository;
        this.playerQuoteRepository = playerQuoteRepository;
        this.performanceStrategy = performanceStrategy;
        this.positionStrategy = positionStrategy;
    }

    @Transactional
    @CacheEvict(value = { "quotes", "ranking", "playerDetail" }, allEntries = true)
    public RecalculateResponse recalculate() {
        log.info("Starting quote recalculation with strategy: {}", activeStrategy);
        List<Player> players = playerRepository.findAll();

        ValuationStrategy strategy = getActiveStrategy();
        AtomicInteger quotesGenerated = new AtomicInteger(0);

        for (Player player : players) {
            try {
                BigDecimal value = strategy.calculate(player);
                PlayerQuote quote = PlayerQuote.builder()
                        .player(player)
                        .value(value)
                        .quoteDate(LocalDateTime.now())
                        .strategyVersion(strategy.getVersion())
                        .baseScore(value)
                        .build();
                playerQuoteRepository.save(quote);
                quotesGenerated.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to calculate quote for player {}: {}", player.getName(), e.getMessage());
            }
        }

        log.info("Quote recalculation complete. {} quotes generated for {} players", quotesGenerated.get(),
                players.size());
        return RecalculateResponse.builder()
                .playersProcessed(players.size())
                .quotesGenerated(quotesGenerated.get())
                .strategyUsed(strategy.getVersion())
                .calculatedAt(LocalDateTime.now())
                .status("SUCCESS")
                .build();
    }

    @Cacheable(value = "quotes", key = "#playerId")
    public List<QuoteDTO> getPlayerQuotes(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        return playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(player)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public QuoteDTO getCurrentQuote(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        return playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("No quote found for player: " + playerId));
    }

    public QuoteDTO getQuoteAtDate(Long playerId, LocalDateTime date) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        List<PlayerQuote> quotes = playerQuoteRepository.findByPlayerAndDateBefore(player, date);
        if (quotes.isEmpty()) {
            throw new ResourceNotFoundException("No quote found for player " + playerId + " before " + date);
        }
        return toDTO(quotes.get(0));
    }

    @Cacheable("ranking")
    public List<PlayerRankingDTO> getRanking() {
        ValuationStrategy strategy = getActiveStrategy();
        List<Player> players = playerRepository.findAll();

        List<PlayerRankingDTO> ranking = new ArrayList<>();
        for (Player player : players) {
            Optional<PlayerQuote> latestQuote = playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player);
            BigDecimal score = strategy.calculate(player);

            ranking.add(PlayerRankingDTO.builder()
                    .playerId(player.getId())
                    .playerName(player.getName())
                    .league(player.getLeague())
                    .team(player.getTeam())
                    .position(player.getPosition())
                    .currentQuote(latestQuote.map(PlayerQuote::getValue).orElse(score))
                    .score(score)
                    .strategyVersion(strategy.getVersion())
                    .build());
        }

        ranking.sort(Comparator.comparing(PlayerRankingDTO::getScore).reversed());

        AtomicInteger rank = new AtomicInteger(1);
        ranking.forEach(r -> r.setRank(rank.getAndIncrement()));

        return ranking;
    }

    public BigDecimal getCurrentPrice(Player player) {
        return playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player)
                .map(PlayerQuote::getValue)
                .orElse(BigDecimal.ONE);
    }

    public void setActiveStrategy(String strategyName) {
        if (!strategyName.equals("performanceBased") && !strategyName.equals("positionWeighted")) {
            throw new IllegalArgumentException("Unknown strategy: " + strategyName);
        }
        this.activeStrategy = strategyName;
        log.info("Active strategy changed to: {}", strategyName);
    }

    private ValuationStrategy getActiveStrategy() {
        return activeStrategy.equals("performanceBased") ? performanceStrategy : positionStrategy;
    }

    private QuoteDTO toDTO(PlayerQuote quote) {
        return QuoteDTO.builder()
                .id(quote.getId())
                .playerId(quote.getPlayer().getId())
                .playerName(quote.getPlayer().getName())
                .value(quote.getValue())
                .quoteDate(quote.getQuoteDate())
                .strategyVersion(quote.getStrategyVersion())
                .baseScore(quote.getBaseScore())
                .build();
    }
}
