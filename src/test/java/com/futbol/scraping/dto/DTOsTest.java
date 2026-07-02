package com.futbol.scraping.dto;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class DTOsTest {

    @Test
    void authResponse_ShouldBuild() {
        AuthResponse response = AuthResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .token("jwt-token")
                .build();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void buyOrderRequest_ShouldBuild() {
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(100L)
                .buyerId(42L)
                .quantity(5)
                .build();

        assertThat(request.getPlayerId()).isEqualTo(100L);
        assertThat(request.getBuyerId()).isEqualTo(42L);
        assertThat(request.getQuantity()).isEqualTo(5);
    }

    @Test
    void sellOrderRequest_ShouldBuild() {
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(100L)
                .sellerId(42L)
                .quantity(3)
                .build();

        assertThat(request.getPlayerId()).isEqualTo(100L);
        assertThat(request.getSellerId()).isEqualTo(42L);
        assertThat(request.getQuantity()).isEqualTo(3);
    }

    @Test
    void createUserRequest_WithBalance() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setBalance(new BigDecimal("5000"));

        assertThat(request.getUsername()).isEqualTo("newuser");
        assertThat(request.getEmail()).isEqualTo("new@example.com");
        assertThat(request.getBalanceOrDefault()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    void createUserRequest_WithNullBalance_ReturnsDefault() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setBalance(null);

        assertThat(request.getBalanceOrDefault()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void registerRequest_ShouldHandleAllFields() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("messi");
        request.setEmail("messi@example.com");
        request.setPassword("secret123");
        request.setInitialBalance(new BigDecimal("10000"));

        assertThat(request.getUsername()).isEqualTo("messi");
        assertThat(request.getEmail()).isEqualTo("messi@example.com");
        assertThat(request.getPassword()).isEqualTo("secret123");
        assertThat(request.getInitialBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void registerRequest_WithNullInitialBalance() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user");
        request.setEmail("user@example.com");
        request.setPassword("pass");

        assertThat(request.getInitialBalance()).isNull();
    }

    @Test
    void orderBookResponse_ShouldBuild() {
        OrderBookResponse response = OrderBookResponse.builder()
                .playerId(100L)
                .playerName("Messi")
                .price(new BigDecimal("125.50"))
                .buyQuantity(15)
                .buyOrderCount(3)
                .sellQuantity(10)
                .sellOrderCount(2)
                .build();

        assertThat(response.getPlayerId()).isEqualTo(100L);
        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("125.50"));
        assertThat(response.getBuyQuantity()).isEqualTo(15);
        assertThat(response.getBuyOrderCount()).isEqualTo(3);
        assertThat(response.getSellQuantity()).isEqualTo(10);
        assertThat(response.getSellOrderCount()).isEqualTo(2);
    }

    @Test
    void orderResponse_ShouldBuild() {
        OrderResponse response = OrderResponse.builder()
                .orderId(5001L)
                .playerId(100L)
                .playerName("Messi")
                .type("BUY")
                .price(new BigDecimal("125.50"))
                .quantity(10)
                .filledQuantity(7)
                .remainingQuantity(3)
                .status("PARTIALLY_FILLED")
                .totalAmount(new BigDecimal("878.50"))
                .build();

        assertThat(response.getOrderId()).isEqualTo(5001L);
        assertThat(response.getPlayerId()).isEqualTo(100L);
        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getType()).isEqualTo("BUY");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("125.50"));
        assertThat(response.getQuantity()).isEqualTo(10);
        assertThat(response.getFilledQuantity()).isEqualTo(7);
        assertThat(response.getRemainingQuantity()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo("PARTIALLY_FILLED");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("878.50"));
    }

    @Test
    void orderResponse_WithZeroValues() {
        OrderResponse response = OrderResponse.builder()
                .orderId(1L)
                .playerId(1L)
                .playerName("Test")
                .type("SELL")
                .price(BigDecimal.ZERO)
                .quantity(0)
                .filledQuantity(0)
                .remainingQuantity(0)
                .status("PENDING")
                .totalAmount(BigDecimal.ZERO)
                .build();

        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getTotalAmount()).isZero();
    }

    @Test
    void playerDTO_ShouldBuild() {
        LocalDateTime now = LocalDateTime.now();
        PlayerDTO dto = PlayerDTO.builder()
                .id(100L)
                .name("Messi")
                .league("La Liga")
                .team("Barcelona")
                .position("FW")
                .age(37)
                .appearances(28)
                .goals(20)
                .assists(12)
                .whoscoredId("ws-100")
                .currentQuote(new BigDecimal("12.75"))
                .lastQuoteDate(now)
                .build();

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getName()).isEqualTo("Messi");
        assertThat(dto.getLeague()).isEqualTo("La Liga");
        assertThat(dto.getTeam()).isEqualTo("Barcelona");
        assertThat(dto.getPosition()).isEqualTo("FW");
        assertThat(dto.getAge()).isEqualTo(37);
        assertThat(dto.getAppearances()).isEqualTo(28);
        assertThat(dto.getGoals()).isEqualTo(20);
        assertThat(dto.getAssists()).isEqualTo(12);
        assertThat(dto.getWhoscoredId()).isEqualTo("ws-100");
        assertThat(dto.getCurrentQuote()).isEqualByComparingTo(new BigDecimal("12.75"));
        assertThat(dto.getLastQuoteDate()).isEqualTo(now);
    }

    @Test
    void playerDetailDTO_ShouldBuild() {
        LocalDateTime now = LocalDateTime.now();
        QuoteDTO quote = QuoteDTO.builder().id(1L).value(new BigDecimal("100")).build();

        PlayerDetailDTO dto = PlayerDetailDTO.builder()
                .id(100L)
                .name("Messi")
                .league("La Liga")
                .team("Barcelona")
                .position("FW")
                .age(37)
                .weight(72)
                .appearances(28)
                .goals(20)
                .assists(12)
                .whoscoredId("ws-100")
                .url("https://example.com")
                .currentQuote(new BigDecimal("100"))
                .lastQuoteDate(now)
                .recentQuotes(List.of(quote))
                .build();

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getName()).isEqualTo("Messi");
        assertThat(dto.getRecentQuotes()).hasSize(1);
        assertThat(dto.getUrl()).isEqualTo("https://example.com");
    }

    @Test
    void playerDetailDTO_WithEmptyQuotes() {
        PlayerDetailDTO dto = PlayerDetailDTO.builder()
                .id(1L)
                .name("Test")
                .recentQuotes(List.of())
                .build();

        assertThat(dto.getRecentQuotes()).isEmpty();
    }

    @Test
    void playerRankingDTO_ShouldBuild() {
        PlayerRankingDTO dto = PlayerRankingDTO.builder()
                .rank(1)
                .playerId(100L)
                .playerName("Messi")
                .league("La Liga")
                .team("Barcelona")
                .position("FW")
                .currentQuote(new BigDecimal("12.75"))
                .score(new BigDecimal("98.4"))
                .strategyVersion("v1")
                .build();

        assertThat(dto.getRank()).isEqualTo(1);
        assertThat(dto.getPlayerId()).isEqualTo(100L);
        assertThat(dto.getPlayerName()).isEqualTo("Messi");
        assertThat(dto.getScore()).isEqualByComparingTo(new BigDecimal("98.4"));
        assertThat(dto.getStrategyVersion()).isEqualTo("v1");
    }

    @Test
    void playerStatsDTO_ShouldBuild() {
        PlayerStatsDTO dto = PlayerStatsDTO.builder()
                .whoscoredId("ws-123")
                .playerId(123L)
                .name("Test Player")
                .firstName("Test")
                .lastName("Player")
                .team("Test FC")
                .teamId(1L)
                .league("Premier League")
                .position("MF")
                .positionText("Midfielder")
                .playedPositions("CM,DM")
                .playedPositionsShort("CM,DM")
                .nationality("England")
                .teamRegionName("England")
                .regionCode("EN")
                .age(25)
                .height(180)
                .weight(75)
                .appearances(30)
                .subOn(5)
                .manOfTheMatch(3)
                .minutesPlayed(2500)
                .goals(10)
                .assists(8)
                .isManOfTheMatch(true)
                .isActive(true)
                .isOpta(true)
                .tournamentShortName("PL")
                .tournamentId(1L)
                .tournamentName("Premier League")
                .tournamentRegionId(2L)
                .tournamentRegionCode("EN")
                .tournamentRegionName("England")
                .seasonId(2024L)
                .seasonName("2024/2025")
                .rating(7.5)
                .shotsPerGame(2.5)
                .aerialWonPerGame(1.5)
                .yellowCard(3.0)
                .redCard(0.0)
                .passSuccess(85.0)
                .ranking(10)
                .url("https://example.com")
                .build();

        assertThat(dto.getName()).isEqualTo("Test Player");
        assertThat(dto.getWhoscoredId()).isEqualTo("ws-123");
        assertThat(dto.getRating()).isEqualTo(7.5);
        assertThat(dto.getIsManOfTheMatch()).isTrue();
    }

    @Test
    void portfolioDTO_ShouldBuild() {
        PortfolioItemDTO item = PortfolioItemDTO.builder()
                .playerId(100L)
                .playerName("Messi")
                .league("La Liga")
                .team("Barcelona")
                .position("FW")
                .quantity(10)
                .avgBuyPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("125.00"))
                .totalInvested(new BigDecimal("1000.00"))
                .currentValue(new BigDecimal("1250.00"))
                .profitLoss(new BigDecimal("250.00"))
                .profitLossPercent(new BigDecimal("25.00"))
                .build();

        PortfolioDTO portfolio = PortfolioDTO.builder()
                .userId(42L)
                .username("messi")
                .totalInvested(new BigDecimal("1000.00"))
                .currentValue(new BigDecimal("1250.00"))
                .profitLoss(new BigDecimal("250.00"))
                .profitLossPercent(new BigDecimal("25.00"))
                .positions(List.of(item))
                .build();

        assertThat(portfolio.getUserId()).isEqualTo(42L);
        assertThat(portfolio.getUsername()).isEqualTo("messi");
        assertThat(portfolio.getPositions()).hasSize(1);
        assertThat(item.getProfitLossPercent()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void portfolioDTO_WithEmptyPositions() {
        PortfolioDTO portfolio = PortfolioDTO.builder()
                .userId(1L)
                .username("test")
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .profitLoss(BigDecimal.ZERO)
                .profitLossPercent(BigDecimal.ZERO)
                .positions(List.of())
                .build();

        assertThat(portfolio.getPositions()).isEmpty();
    }

    @Test
    void portfolioItemDTO_WithZeroValues() {
        PortfolioItemDTO item = PortfolioItemDTO.builder()
                .playerId(1L)
                .playerName("Test")
                .quantity(0)
                .avgBuyPrice(BigDecimal.ZERO)
                .currentPrice(BigDecimal.ZERO)
                .totalInvested(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .profitLoss(BigDecimal.ZERO)
                .profitLossPercent(BigDecimal.ZERO)
                .build();

        assertThat(item.getQuantity()).isZero();
        assertThat(item.getTotalInvested()).isZero();
    }

    @Test
    void quoteDTO_ShouldBuild() {
        LocalDateTime now = LocalDateTime.now();
        QuoteDTO dto = QuoteDTO.builder()
                .id(5001L)
                .playerId(100L)
                .playerName("Messi")
                .value(new BigDecimal("12.75"))
                .quoteDate(now)
                .strategyVersion("v1")
                .baseScore(new BigDecimal("98.4"))
                .build();

        assertThat(dto.getId()).isEqualTo(5001L);
        assertThat(dto.getPlayerId()).isEqualTo(100L);
        assertThat(dto.getValue()).isEqualByComparingTo(new BigDecimal("12.75"));
        assertThat(dto.getQuoteDate()).isEqualTo(now);
        assertThat(dto.getBaseScore()).isEqualByComparingTo(new BigDecimal("98.4"));
    }

    @Test
    void recalculateResponse_ShouldBuild() {
        LocalDateTime now = LocalDateTime.now();
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(2757)
                .quotesGenerated(2757)
                .strategyUsed("performance-v1.0")
                .calculatedAt(now)
                .status("SUCCESS")
                .build();

        assertThat(response.getPlayersProcessed()).isEqualTo(2757);
        assertThat(response.getQuotesGenerated()).isEqualTo(2757);
        assertThat(response.getStrategyUsed()).isEqualTo("performance-v1.0");
        assertThat(response.getCalculatedAt()).isEqualTo(now);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void recalculateResponse_WithZeroCounts() {
        RecalculateResponse response = RecalculateResponse.builder()
                .playersProcessed(0)
                .quotesGenerated(0)
                .strategyUsed("v1")
                .status("SUCCESS")
                .build();

        assertThat(response.getPlayersProcessed()).isZero();
        assertThat(response.getQuotesGenerated()).isZero();
    }

    @Test
    void syncPlayersResponse_ShouldCreate() {
        SyncPlayersResponse response = new SyncPlayersResponse(42, "SUCCESS");

        assertThat(response.playersSync()).isEqualTo(42);
        assertThat(response.status()).isEqualTo("SUCCESS");
    }

    @Test
    void syncPlayersResponse_WithZero() {
        SyncPlayersResponse response = new SyncPlayersResponse(0, "SUCCESS");

        assertThat(response.playersSync()).isZero();
    }

    @Test
    void transactionDTO_ShouldBuild() {
        LocalDateTime now = LocalDateTime.now();
        TransactionDTO dto = TransactionDTO.builder()
                .id(7001L)
                .playerId(100L)
                .playerName("Messi")
                .type(Transaction.TransactionType.BUY)
                .quantity(5)
                .pricePerToken(new BigDecimal("12.75"))
                .totalAmount(new BigDecimal("63.75"))
                .createdAt(now)
                .build();

        assertThat(dto.getId()).isEqualTo(7001L);
        assertThat(dto.getPlayerId()).isEqualTo(100L);
        assertThat(dto.getPlayerName()).isEqualTo("Messi");
        assertThat(dto.getType()).isEqualTo(Transaction.TransactionType.BUY);
        assertThat(dto.getQuantity()).isEqualTo(5);
        assertThat(dto.getPricePerToken()).isEqualByComparingTo(new BigDecimal("12.75"));
        assertThat(dto.getTotalAmount()).isEqualByComparingTo(new BigDecimal("63.75"));
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void transactionDTO_WithSellType() {
        TransactionDTO dto = TransactionDTO.builder()
                .id(1L)
                .playerId(1L)
                .playerName("Test")
                .type(Transaction.TransactionType.SELL)
                .quantity(1)
                .pricePerToken(BigDecimal.ONE)
                .totalAmount(BigDecimal.ONE)
                .build();

        assertThat(dto.getType()).isEqualTo(Transaction.TransactionType.SELL);
    }

    @Test
    void userCreationResponse_ShouldCreate() {
        UserCreationResponse response = new UserCreationResponse(1L, "testuser", "test@example.com", new BigDecimal("10000"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void userCreationResponse_WithZeroBalance() {
        UserCreationResponse response = new UserCreationResponse(2L, "user2", "user2@test.com", BigDecimal.ZERO);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.balance()).isZero();
    }

    @Test
    void playerStatsDTO_WithNullBooleanFields() {
        PlayerStatsDTO dto = PlayerStatsDTO.builder()
                .name("Test")
                .build();

        assertThat(dto.getName()).isEqualTo("Test");
        assertThat(dto.getIsManOfTheMatch()).isNull();
        assertThat(dto.getIsActive()).isNull();
        assertThat(dto.getIsOpta()).isNull();
    }

    @Test
    void playerDTO_WithNullQuote() {
        PlayerDTO dto = PlayerDTO.builder()
                .id(1L)
                .name("Test")
                .currentQuote(null)
                .lastQuoteDate(null)
                .build();

        assertThat(dto.getCurrentQuote()).isNull();
        assertThat(dto.getLastQuoteDate()).isNull();
    }
}
