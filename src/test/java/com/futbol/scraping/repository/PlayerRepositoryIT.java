package com.futbol.scraping.repository;

import com.futbol.scraping.annotation.FutbolJpaIT;
import com.futbol.scraping.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolJpaIT
class PlayerRepositoryIT {

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void save_andFindById_returnsPlayer() {
        Player player = Player.builder()
                .name("Lionel Messi")
                .league("La Liga")
                .team("Barcelona")
                .position("FW")
                .build();

        Player saved = playerRepository.save(player);
        Optional<Player> found = playerRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Lionel Messi");
        assertThat(found.get().getLeague()).isEqualTo("La Liga");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void findAll_returnsAllSavedPlayers() {
        playerRepository.save(Player.builder().name("Messi").league("La Liga").team("Barcelona").position("FW").build());
        playerRepository.save(Player.builder().name("Ronaldo").league("Premier League").team("Man Utd").position("FW").build());

        List<Player> players = playerRepository.findAll();

        assertThat(players).hasSize(2);
        assertThat(players).extracting(Player::getName)
                .containsExactlyInAnyOrder("Messi", "Ronaldo");
    }

    @Test
    void findByWhoscoredId_returnsMatchingPlayer() {
        playerRepository.save(Player.builder()
                .name("Benzema")
                .league("La Liga")
                .team("Real Madrid")
                .position("FW")
                .whoscoredId("ws-456")
                .build());

        Optional<Player> found = playerRepository.findByWhoscoredId("ws-456");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Benzema");
    }

    @Test
    void findByWhoscoredId_withUnknownId_returnsEmpty() {
        assertThat(playerRepository.findByWhoscoredId("ws-inexistente")).isEmpty();
    }

    @Test
    void existsByWhoscoredId_returnsTrueWhenExists() {
        playerRepository.save(Player.builder()
                .name("Xavi")
                .league("La Liga")
                .team("Barcelona")
                .position("CM")
                .whoscoredId("ws-789")
                .build());

        assertThat(playerRepository.existsByWhoscoredId("ws-789")).isTrue();
        assertThat(playerRepository.existsByWhoscoredId("ws-000")).isFalse();
    }
}
