package com.futbol.scraping.strategy;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.model.Player;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class ValuationStrategyTest {

    @Test
    void performanceStrategyImplementsValuationStrategy() {
        ValuationStrategy strategy = new PerformanceBasedStrategy();
        assertThat(strategy).isInstanceOf(ValuationStrategy.class);
    }

    @Test
    void positionWeightedStrategyImplementsValuationStrategy() {
        ValuationStrategy strategy = new PositionWeightedStrategy();
        assertThat(strategy).isInstanceOf(ValuationStrategy.class);
    }

    @Test
    void bothStrategiesCalculateValidValues() {
        ValuationStrategy perf = new PerformanceBasedStrategy();
        ValuationStrategy pos = new PositionWeightedStrategy();
        Player player = Player.builder()
                .name("Test")
                .goals(10)
                .assists(5)
                .appearances(20)
                .position("ST")
                .build();

        BigDecimal perfResult = perf.calculate(player);
        BigDecimal posResult = pos.calculate(player);

        assertThat(perfResult).isNotNull();
        assertThat(posResult).isNotNull();
    }

    @Test
    void bothStrategiesHaveDifferentVersions() {
        ValuationStrategy perf = new PerformanceBasedStrategy();
        ValuationStrategy pos = new PositionWeightedStrategy();

        assertThat(perf.getVersion()).isNotEqualTo(pos.getVersion());
    }
}
