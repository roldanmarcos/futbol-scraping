package com.futbol.scraping.repository;

import com.futbol.scraping.annotation.FutbolJpaIT;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.Transaction;
import com.futbol.scraping.model.Transaction.TransactionType;
import com.futbol.scraping.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolJpaIT
class TransactionRepositoryIT {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private User userA;
    private User userB;
    private Player messi;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        playerRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder().username("userA").email("a@test.com").passwordHash("h").balance(new BigDecimal("5000")).build());
        userB = userRepository.save(User.builder().username("userB").email("b@test.com").passwordHash("h").balance(new BigDecimal("3000")).build());
        messi = playerRepository.save(Player.builder().name("Messi").league("MLS").team("Inter Miami").position("FW").build());

        transactionRepository.save(Transaction.builder()
                .user(userA).player(messi).transactionType(TransactionType.BUY)
                .quantity(5).pricePerToken(new BigDecimal("100.00")).totalAmount(new BigDecimal("500.00")).build());
        transactionRepository.save(Transaction.builder()
                .user(userA).player(messi).transactionType(TransactionType.SELL)
                .quantity(2).pricePerToken(new BigDecimal("120.00")).totalAmount(new BigDecimal("240.00")).build());
        transactionRepository.save(Transaction.builder()
                .user(userB).player(messi).transactionType(TransactionType.BUY)
                .quantity(3).pricePerToken(new BigDecimal("100.00")).totalAmount(new BigDecimal("300.00")).build());
    }

    @Test
    void findByUserOrderByCreatedAtDesc_returnsOnlyUserTransactions() {
        List<Transaction> txs = transactionRepository.findByUserOrderByCreatedAtDesc(userA);

        assertThat(txs).hasSize(2);
        assertThat(txs).extracting(Transaction::getUser)
                .allMatch(u -> u.getId().equals(userA.getId()));
    }

    @Test
    void findByUserOrderByCreatedAtDesc_returnsInDescendingOrder() {
        List<Transaction> txs = transactionRepository.findByUserOrderByCreatedAtDesc(userA);

        assertThat(txs.get(0).getCreatedAt()).isAfterOrEqualTo(txs.get(1).getCreatedAt());
    }

    @Test
    void findByUserOrderByCreatedAtDesc_withUserWithNoTransactions_returnsEmpty() {
        User emptyUser = userRepository.save(User.builder().username("empty").email("empty@test.com").passwordHash("h").balance(BigDecimal.ZERO).build());

        assertThat(transactionRepository.findByUserOrderByCreatedAtDesc(emptyUser)).isEmpty();
    }

    @Test
    void findByUserOrderByCreatedAtDesc_includesCorrectTransactionTypes() {
        List<Transaction> txs = transactionRepository.findByUserOrderByCreatedAtDesc(userA);

        assertThat(txs).extracting(Transaction::getTransactionType)
                .containsExactlyInAnyOrder(TransactionType.BUY, TransactionType.SELL);
    }
}
