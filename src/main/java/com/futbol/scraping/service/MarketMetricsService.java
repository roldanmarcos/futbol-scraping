package com.futbol.scraping.service;

import com.futbol.scraping.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketMetricsService {

    private final MeterRegistry meterRegistry;
    private final UserRepository userRepository;

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("market.total_credits", userRepository,
                        u -> u.sumBalances().doubleValue())
                .description("Suma total de saldos de todos los usuarios")
                .register(meterRegistry);

        log.info("Market metrics registered: market.total_credits");
    }
}
