package com.futbol.scraping.integration;

import com.futbol.scraping.dto.PlayerRankingDTO;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerQuote;
import com.futbol.scraping.repository.PlayerQuoteRepository;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.strategy.ValuationStrategy;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("integration")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {QuoteService.class, RankingCacheIT.CacheTestConfig.class})
class RankingCacheIT {

    @MockBean
    private PlayerRepository playerRepository;

    @MockBean
    private PlayerQuoteRepository playerQuoteRepository;

    @MockBean(name = "performanceBased")
    private ValuationStrategy performanceStrategy;

    @MockBean(name = "positionWeighted")
    private ValuationStrategy positionStrategy;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private CacheManager cacheManager;

    private Player messi;
    private PlayerQuote messiQuote;

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(cacheManager.getCache("ranking")).clear();

        messi = Player.builder().id(1L).name("Messi").league("La Liga").team("Barcelona").position("FW").build();
        messiQuote = PlayerQuote.builder()
                .id(1L).player(messi)
                .value(new BigDecimal("150.00"))
                .quoteDate(LocalDateTime.now())
                .strategyVersion("v1.0")
                .baseScore(new BigDecimal("150.00"))
                .build();
    }

    @Test
    void getRanking_secondCall_returnsCachedResultWithoutHittingDb() {
        when(playerRepository.findAll()).thenReturn(List.of(messi));
        when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(messi)).thenReturn(Optional.of(messiQuote));

        List<PlayerRankingDTO> first = quoteService.getRanking();
        List<PlayerRankingDTO> second = quoteService.getRanking();

        assertThat(first).isEqualTo(second);
        verify(playerRepository, times(1)).findAll();
        verify(playerQuoteRepository, times(1)).findTopByPlayerOrderByQuoteDateDesc(messi);
    }

    @Test
    void recalculate_evictsRankingCache_nextGetHitsDb() {
        when(playerRepository.findAll()).thenReturn(List.of(messi));
        when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(messi)).thenReturn(Optional.of(messiQuote));
        when(performanceStrategy.calculate(messi)).thenReturn(new BigDecimal("150.00"));
        when(performanceStrategy.getVersion()).thenReturn("v1.0");
        when(playerQuoteRepository.save(any())).thenReturn(messiQuote);

        quoteService.getRanking();
        quoteService.recalculate();
        quoteService.getRanking();

        // playerRepository.findAll() called once per: getRanking, recalculate, getRanking
        verify(playerRepository, times(3)).findAll();
    }

    @Test
    void setActiveStrategy_evictsRankingCache_nextGetHitsDb() {
        when(playerRepository.findAll()).thenReturn(List.of(messi));
        when(playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(messi)).thenReturn(Optional.of(messiQuote));

        quoteService.getRanking();
        quoteService.setActiveStrategy("positionWeighted");
        quoteService.getRanking();

        verify(playerRepository, times(2)).findAll();
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        public CacheManager cacheManager() {
            CaffeineCacheManager manager = new CaffeineCacheManager();
            manager.setCaffeine(Caffeine.newBuilder().maximumSize(500));
            manager.registerCustomCache("ranking", Caffeine.newBuilder().maximumSize(1).build());
            return manager;
        }
    }
}
