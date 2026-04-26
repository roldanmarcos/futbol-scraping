package com.futbol.scraping.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerQuoteTest {

    @Test
    void onCreateShouldSetCreatedAt() {
        PlayerQuote quote = PlayerQuote.builder().build();

        quote.onCreate();

        assertThat(quote.getCreatedAt()).isNotNull();
    }
}

