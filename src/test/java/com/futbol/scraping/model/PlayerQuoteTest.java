package com.futbol.scraping.model;

import com.futbol.scraping.annotation.FutbolUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class PlayerQuoteTest {

    @Test
    void onCreateShouldSetCreatedAt() {
        PlayerQuote quote = PlayerQuote.builder().build();

        quote.onCreate();

        assertThat(quote.getCreatedAt()).isNotNull();
    }
}

