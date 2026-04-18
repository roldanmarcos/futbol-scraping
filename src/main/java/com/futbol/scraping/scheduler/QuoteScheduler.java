package com.futbol.scraping.scheduler;

import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.service.ScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteScheduler {

    private final QuoteService quoteService;
    private final ScrapingService scrapingService;

    @Value("${app.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Scheduled(cron = "${app.scheduling.cron:0 0 0 * * MON}")
    public void recalculateQuotes() {
        if (!schedulingEnabled) {
            log.debug("Scheduling is disabled, skipping quote recalculation");
            return;
        }
        log.info("Scheduled quote recalculation started");
        try {
            RecalculateResponse response = quoteService.recalculate();
            log.info("Scheduled quote recalculation completed: {} quotes for {} players, strategy: {}",
                    response.getQuotesGenerated(), response.getPlayersProcessed(), response.getStrategyUsed());
        } catch (Exception e) {
            log.error("Scheduled quote recalculation failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 22 * * SUN")
    public void syncExternalData() {
        if (!schedulingEnabled) {
            return;
        }
        log.info("Scheduled data sync started");
        try {
            int synced = scrapingService.syncAllLeagues();
            log.info("Scheduled data sync completed: {} players synced", synced);
        } catch (Exception e) {
            log.error("Scheduled data sync failed: {}", e.getMessage(), e);
        }
    }
}
