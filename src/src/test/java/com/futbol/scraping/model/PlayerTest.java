package com.futbol.scraping.model;

import com.futbol.scraping.annotation.FutbolUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class PlayerTest {

    @Test
    void onCreateShouldInitializeTimestamps() {
        Player player = Player.builder().name("Test Player").build();

        player.onCreate();

        assertThat(player.getCreatedAt()).isNotNull().isGreaterThan(0L);
        assertThat(player.getUpdatedAt()).isNotNull().isGreaterThan(0L);
        assertThat(player.getLastScrapedAt()).isNotNull().isGreaterThan(0L);
    }

    @Test
    void onUpdateShouldRefreshUpdatedAt() {
        Player player = Player.builder().name("Test Player").updatedAt(0L).build();

        player.onUpdate();

        assertThat(player.getUpdatedAt()).isNotNull().isGreaterThan(0L);
    }
}

