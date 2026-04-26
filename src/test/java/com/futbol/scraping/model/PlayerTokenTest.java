package com.futbol.scraping.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerTokenTest {

    @Test
    void onCreateShouldSetCreatedAtAndUpdatedAt() {
        PlayerToken token = PlayerToken.builder().build();

        token.onCreate();

        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdateShouldRefreshUpdatedAt() {
        LocalDateTime oldDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        PlayerToken token = PlayerToken.builder().updatedAt(oldDate).build();

        token.onUpdate();

        assertThat(token.getUpdatedAt()).isAfter(oldDate);
    }
}

