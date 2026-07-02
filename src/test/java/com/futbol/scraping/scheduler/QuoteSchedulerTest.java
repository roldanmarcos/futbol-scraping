package com.futbol.scraping.scheduler;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.dto.RecalculateResponse;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.service.ScrapingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@FutbolUnit
@ExtendWith(MockitoExtension.class)
class QuoteSchedulerTest {

    @Mock
    private QuoteService quoteService;

    @Mock
    private ScrapingService scrapingService;

    private QuoteScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new QuoteScheduler(quoteService, scrapingService);
        ReflectionTestUtils.setField(scheduler, "schedulingEnabled", true);
    }

    @Test
    void recalculateQuotes_WhenEnabled_ShouldCallService() {
        when(quoteService.recalculate()).thenReturn(
                RecalculateResponse.builder()
                        .quotesGenerated(10)
                        .playersProcessed(5)
                        .strategyUsed("v1")
                        .build());

        scheduler.recalculateQuotes();

        verify(quoteService).recalculate();
    }

    @Test
    void recalculateQuotes_WhenDisabled_ShouldSkip() {
        ReflectionTestUtils.setField(scheduler, "schedulingEnabled", false);

        scheduler.recalculateQuotes();

        verify(quoteService, never()).recalculate();
    }

    @Test
    void recalculateQuotes_WhenException_ShouldNotPropagate() {
        when(quoteService.recalculate()).thenThrow(new RuntimeException("Service error"));

        scheduler.recalculateQuotes();

        verify(quoteService).recalculate();
    }

    @Test
    void syncExternalData_WhenEnabled_ShouldCallService() {
        when(scrapingService.syncAllLeagues()).thenReturn(42);

        scheduler.syncExternalData();

        verify(scrapingService).syncAllLeagues();
    }

    @Test
    void syncExternalData_WhenDisabled_ShouldSkip() {
        ReflectionTestUtils.setField(scheduler, "schedulingEnabled", false);

        scheduler.syncExternalData();

        verify(scrapingService, never()).syncAllLeagues();
    }

    @Test
    void syncExternalData_WhenException_ShouldNotPropagate() {
        when(scrapingService.syncAllLeagues()).thenThrow(new RuntimeException("Sync error"));

        scheduler.syncExternalData();

        verify(scrapingService).syncAllLeagues();
    }
}
