package com.futbol.scraping.model;

import com.futbol.scraping.annotation.FutbolUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class UserTest {

    @Test
    void onCreateShouldInitializeDatesAndDefaultSuperuser() {
        User user = User.builder()
                .username("test")
                .email("test@example.com")
                .balance(BigDecimal.TEN)
                .build();

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getIsSuperuser()).isFalse();
    }

    @Test
    void onCreateShouldKeepProvidedSuperuserValue() {
        User user = User.builder()
                .username("admin")
                .email("admin@example.com")
                .balance(BigDecimal.TEN)
                .isSuperuser(true)
                .build();

        user.onCreate();

        assertThat(user.getIsSuperuser()).isTrue();
    }

    @Test
    void onUpdateShouldRefreshUpdatedAt() {
        LocalDateTime oldDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        User user = User.builder().updatedAt(oldDate).build();

        user.onUpdate();

        assertThat(user.getUpdatedAt()).isAfter(oldDate);
    }
}

