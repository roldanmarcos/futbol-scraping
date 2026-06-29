package com.futbol.scraping.integration;

import com.futbol.scraping.annotation.FutbolIT;
import com.futbol.scraping.dto.AuthResponse;
import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.PortfolioDTO;
import com.futbol.scraping.dto.RegisterRequest;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerToken;
import com.futbol.scraping.model.User;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.repository.PlayerTokenRepository;
import com.futbol.scraping.repository.TradeOrderRepository;
import com.futbol.scraping.repository.TransactionRepository;
import com.futbol.scraping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolIT
class OrderFlowIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerTokenRepository playerTokenRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Player messi;
    private String jwtToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        tradeOrderRepository.deleteAll();
        transactionRepository.deleteAll();
        playerTokenRepository.deleteAll();
        playerRepository.deleteAll();
        userRepository.deleteAll();

        messi = playerRepository.save(Player.builder()
                .name("Lionel Messi").league("MLS").team("Inter Miami").position("FW").build());

        // El servicio de compra requiere un superuser con tokens del jugador (pool de venta)
        User superuser = userRepository.save(User.builder()
                .username("superuser")
                .email("super@futbol.com")
                .passwordHash(passwordEncoder.encode("superpass"))
                .balance(new BigDecimal("99999999"))
                .isSuperuser(true)
                .build());
        playerTokenRepository.save(PlayerToken.builder()
                .player(messi).user(superuser).quantity(1000).avgBuyPrice(BigDecimal.ONE).build());

        // Registrar el usuario trader vía API para obtener JWT real
        RegisterRequest register = new RegisterRequest();
        register.setUsername("trader");
        register.setEmail("trader@test.com");
        register.setPassword("password123");

        ResponseEntity<AuthResponse> auth = restTemplate.postForEntity("/auth/register", register, AuthResponse.class);
        jwtToken = auth.getBody().getToken();
        userId = auth.getBody().getId();
    }

    @Test
    void buy_createsTransactionAndTokenPosition() {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(messi.getId()).buyerId(userId).quantity(5).build();

        ResponseEntity<OrderResponse> response = postWithAuth("/orders/buy", request, OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transactionRepository.findAll()).hasSize(1);
        assertThat(playerTokenRepository.findByUser(userRepository.findById(userId).get())).hasSize(1);
        assertThat(playerTokenRepository.findByUser(userRepository.findById(userId).get()).get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void buy_reducesUserBalance() {
        BigDecimal balanceBefore = userRepository.findById(userId).get().getBalance();

        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(messi.getId()).buyerId(userId).quantity(3).build();

        postWithAuth("/orders/buy", request, OrderResponse.class);

        BigDecimal balanceAfter = userRepository.findById(userId).get().getBalance();
        assertThat(balanceAfter).isLessThan(balanceBefore);
    }

    @Test
    void buy_portfolioReflectsPosition() {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(messi.getId()).buyerId(userId).quantity(4).build();

        postWithAuth("/orders/buy", request, OrderResponse.class);

        ResponseEntity<PortfolioDTO> portfolio = getWithAuth("/users/" + userId + "/portfolio", PortfolioDTO.class);

        assertThat(portfolio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(portfolio.getBody().getPositions()).hasSize(1);
        assertThat(portfolio.getBody().getPositions().get(0).getPlayerName()).isEqualTo("Lionel Messi");
        assertThat(portfolio.getBody().getPositions().get(0).getQuantity()).isEqualTo(4);
    }

    @Test
    void portfolio_withoutJwt_returns401or403() {
        ResponseEntity<String> response = restTemplate.getForEntity("/users/" + userId + "/portfolio", String.class);

        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    private <T> ResponseEntity<T> postWithAuth(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.set("Content-Type", "application/json");
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
    }

    private <T> ResponseEntity<T> getWithAuth(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }
}
