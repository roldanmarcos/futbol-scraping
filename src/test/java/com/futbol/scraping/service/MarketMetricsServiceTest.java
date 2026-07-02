package com.futbol.scraping.service;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@FutbolUnit
class MarketMetricsServiceTest {

    @Test
    void registerMetrics_ShouldRegisterGauge() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.sumBalances()).thenReturn(BigDecimal.valueOf(50000));

        MarketMetricsService service = new MarketMetricsService(meterRegistry, userRepository);
        ReflectionTestUtils.invokeMethod(service, "registerMetrics");

        assertThat(meterRegistry.find("market.total_credits").gauge()).isNotNull();
    }

    @Test
    void gauge_ShouldReflectSumBalances() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.sumBalances()).thenReturn(BigDecimal.valueOf(100000));

        MarketMetricsService service = new MarketMetricsService(meterRegistry, userRepository);
        ReflectionTestUtils.invokeMethod(service, "registerMetrics");

        Double value = meterRegistry.find("market.total_credits").gauge().value();
        assertThat(value).isEqualTo(100000.0);
    }
}
