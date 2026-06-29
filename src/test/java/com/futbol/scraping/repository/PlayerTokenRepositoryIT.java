package com.futbol.scraping.repository;

import com.futbol.scraping.annotation.FutbolJpaIT;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerToken;
import com.futbol.scraping.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolJpaIT
class PlayerTokenRepositoryIT {

    @Autowired
    private PlayerTokenRepository playerTokenRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserRepository userRepository;

    private Player messi;
    private Player ronaldo;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        playerTokenRepository.deleteAll();
        playerRepository.deleteAll();
        userRepository.deleteAll();

        messi = playerRepository.save(Player.builder().name("Messi").league("MLS").team("Inter Miami").position("FW").build());
        ronaldo = playerRepository.save(Player.builder().name("Ronaldo").league("Saudi Pro").team("Al Nassr").position("FW").build());
        userA = userRepository.save(User.builder().username("userA").email("a@test.com").passwordHash("h").balance(new BigDecimal("5000")).build());
        userB = userRepository.save(User.builder().username("userB").email("b@test.com").passwordHash("h").balance(new BigDecimal("3000")).build());

        playerTokenRepository.save(PlayerToken.builder().player(messi).user(userA).quantity(10).avgBuyPrice(new BigDecimal("100.00")).build());
        playerTokenRepository.save(PlayerToken.builder().player(ronaldo).user(userA).quantity(5).avgBuyPrice(new BigDecimal("80.00")).build());
        playerTokenRepository.save(PlayerToken.builder().player(messi).user(userB).quantity(3).avgBuyPrice(new BigDecimal("110.00")).build());
    }

    @Test
    void findByPlayerAndUser_returnsToken() {
        Optional<PlayerToken> found = playerTokenRepository.findByPlayerAndUser(messi, userA);

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(10);
        assertThat(found.get().getAvgBuyPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void findByPlayerAndUser_withNoPosition_returnsEmpty() {
        assertThat(playerTokenRepository.findByPlayerAndUser(ronaldo, userB)).isEmpty();
    }

    @Test
    void findByUser_returnsAllTokensOfUser() {
        List<PlayerToken> tokens = playerTokenRepository.findByUser(userA);

        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(t -> t.getPlayer().getName())
                .containsExactlyInAnyOrder("Messi", "Ronaldo");
    }

    @Test
    void findByUser_withUserWithNoTokens_returnsEmpty() {
        User emptyUser = userRepository.save(User.builder().username("empty").email("empty@test.com").passwordHash("h").balance(BigDecimal.ZERO).build());

        assertThat(playerTokenRepository.findByUser(emptyUser)).isEmpty();
    }

    @Test
    void sumQuantityByPlayer_returnsCorrectTotal() {
        // messi tiene: userA=10, userB=3 → total 13
        Integer total = playerTokenRepository.sumQuantityByPlayer(messi);

        assertThat(total).isEqualTo(13);
    }

    @Test
    void sumQuantityByPlayer_withNoTokens_returnsZero() {
        Player newPlayer = playerRepository.save(Player.builder().name("Neymar").league("MLS").team("Santos").position("FW").build());

        assertThat(playerTokenRepository.sumQuantityByPlayer(newPlayer)).isEqualTo(0);
    }

    @Test
    void findByPlayerAndUserWithLock_returnsToken() {
        Optional<PlayerToken> found = playerTokenRepository.findByPlayerAndUserWithLock(messi, userA);

        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(10);
    }
}
