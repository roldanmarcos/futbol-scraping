package com.futbol.scraping.model;

import com.futbol.scraping.annotation.FutbolUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class TransactionTest {

    @Test
    void onCreateShouldSetCreatedAt() {
        Transaction transaction = Transaction.builder().build();

        transaction.onCreate();

        assertThat(transaction.getCreatedAt()).isNotNull();
    }

    @Test
    void transactionTypeShouldContainBuyAndSell() {
        assertThat(Transaction.TransactionType.values())
                .containsExactly(Transaction.TransactionType.BUY, Transaction.TransactionType.SELL);
    }
}

