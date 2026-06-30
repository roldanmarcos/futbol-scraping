package com.futbol.scraping.integration;

import com.futbol.scraping.annotation.FutbolIT;
import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.PlayerDetailDTO;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.repository.PlayerTokenRepository;
import com.futbol.scraping.repository.TradeOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolIT
class PlayerControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerTokenRepository playerTokenRepository;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    private Player messi;

    @BeforeEach
    void setUp() {
        tradeOrderRepository.deleteAll();
        playerTokenRepository.deleteAll();
        playerRepository.deleteAll();
        messi = playerRepository.save(Player.builder()
                .name("Lionel Messi").league("La Liga").team("Barcelona").position("FW").build());
        playerRepository.save(Player.builder()
                .name("Cristiano Ronaldo").league("Premier League").team("Man Utd").position("FW").build());
        playerRepository.save(Player.builder()
                .name("Lautaro Martínez").league("Serie A").team("Inter").position("FW").build());
    }

    @Test
    void getPlayers_returnsAllPlayersFromH2() {
        ResponseEntity<PlayerDTO[]> response = restTemplate.getForEntity("/players", PlayerDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody())
                .extracting(PlayerDTO::getName)
                .containsExactlyInAnyOrder("Lionel Messi", "Cristiano Ronaldo", "Lautaro Martínez");
    }

    @Test
    void getPlayers_filtersByLeague_returnsOnlyMatching() {
        ResponseEntity<PlayerDTO[]> response = restTemplate.getForEntity("/players?league=La Liga", PlayerDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getName()).isEqualTo("Lionel Messi");
        assertThat(response.getBody()[0].getLeague()).isEqualTo("La Liga");
    }

    @Test
    void getPlayers_filtersByTeam_returnsOnlyMatching() {
        ResponseEntity<PlayerDTO[]> response = restTemplate.getForEntity("/players?team=Inter", PlayerDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getName()).isEqualTo("Lautaro Martínez");
    }

    @Test
    void getPlayer_byId_returnsDetailFromH2() {
        ResponseEntity<PlayerDetailDTO> response = restTemplate.getForEntity(
                "/players/" + messi.getId(), PlayerDetailDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(messi.getId());
        assertThat(response.getBody().getName()).isEqualTo("Lionel Messi");
        assertThat(response.getBody().getLeague()).isEqualTo("La Liga");
        assertThat(response.getBody().getTeam()).isEqualTo("Barcelona");
    }

    @Test
    void getPlayer_withNonExistentId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/players/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
