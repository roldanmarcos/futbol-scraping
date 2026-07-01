package com.futbol.scraping.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableCaching
@EnableRetry
@EnableScheduling
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Override: ranking cache has no TTL — it lives until an explicit @CacheEvict fires
     * (on recalculate or strategy change). All other caches use the global Caffeine spec
     * defined in application.yml (maximumSize=500, expireAfterWrite=300s).
     */
    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> rankingCacheCustomizer() {
        return manager -> manager.registerCustomCache(
                "ranking",
                Caffeine.newBuilder().maximumSize(1).recordStats().build());
    }
}
