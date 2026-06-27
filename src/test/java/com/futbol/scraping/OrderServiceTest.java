package com.futbol.scraping;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.*;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.repository.PlayerTokenRepository;
import com.futbol.scraping.repository.TransactionRepository;
import com.futbol.scraping.repository.UserRepository;
import com.futbol.scraping.service.OrderService;
import com.futbol.scraping.service.QuoteService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@FutbolUnit
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerTokenRepository playerTokenRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private OrderService orderService;

    private User buyer;
    private User superuser;
    private Player player;
    private PlayerToken superuserToken;
    private PlayerToken buyerToken;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        buyer = User.builder()
                .id(1L)
                .username("buyer")
                .email("buyer@example.com")
                .balance(new BigDecimal("1000.00"))
                .isSuperuser(false)
                .build();

        superuser = User.builder()
                .id(2L)
                .username("superuser")
                .email("super@example.com")
                .balance(new BigDecimal("100000.00"))
                .isSuperuser(true)
                .build();

        player = Player.builder()
                .id(1L)
                .name("Cristiano Ronaldo")
                .league("Serie A")
                .team("Juventus")
                .position("ST")
                .build();

        superuserToken = PlayerToken.builder()
                .id(1L)
                .player(player)
                .user(superuser)
                .quantity(100)
                .avgBuyPrice(BigDecimal.ZERO)
                .build();

        buyerToken = PlayerToken.builder()
                .id(2L)
                .player(player)
                .user(buyer)
                .quantity(10)
                .avgBuyPrice(new BigDecimal("50.00"))
                .build();

        transaction = Transaction.builder()
                .id(1L)
                .user(buyer)
                .player(player)
                .transactionType(Transaction.TransactionType.BUY)
                .quantity(5)
                .pricePerToken(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(orderService, "buySuccessCounter", counter);
    }

    @Test
    void testBuy_Success() {
        // Arrange
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(1L)
                .buyerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                .thenReturn(Optional.of(superuserToken));
        when(playerTokenRepository.findByPlayerAndUser(player, buyer)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        OrderResponse response = orderService.buy(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getPlayerName()).isEqualTo("Cristiano Ronaldo");
        assertThat(response.getType()).isEqualTo("BUY");
        assertThat(response.getQuantity()).isEqualTo(5);
        assertThat(response.getPricePerToken()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));

        verify(playerRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(playerTokenRepository).findByPlayerAndUserWithLock(player, superuser);
        verify(playerTokenRepository, times(2)).save(any(PlayerToken.class));
        verify(userRepository).save(any(User.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testBuy_InsufficientBalance() {
        // Arrange
        buyer.setBalance(new BigDecimal("50.00"));
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(1L)
                .buyerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                .thenReturn(Optional.of(superuserToken));

        // Act & Assert
        assertThatThrownBy(() -> orderService.buy(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient balance");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testBuy_InsufficientTokens() {
        // Arrange
        superuserToken.setQuantity(2);
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(1L)
                .buyerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                .thenReturn(Optional.of(superuserToken));

        // Act & Assert
        assertThatThrownBy(() -> orderService.buy(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Not enough tokens");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testBuy_InvalidQuantity() {
        // Arrange
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(1L)
                .buyerId(1L)
                .quantity(-5)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> orderService.buy(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity must be positive");

        verify(playerRepository, never()).findById(any());
    }

    @Test
    void testBuy_PlayerNotFound() {
        // Arrange
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(999L)
                .buyerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.buy(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void testBuy_UpdateExistingBuyerToken() {
        // Arrange
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(1L)
                .buyerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                .thenReturn(Optional.of(superuserToken));
        when(playerTokenRepository.findByPlayerAndUser(player, buyer)).thenReturn(Optional.of(buyerToken));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        OrderResponse response = orderService.buy(request);

        // Assert
        assertThat(response.getQuantity()).isEqualTo(5);
        verify(playerTokenRepository, times(2)).save(any(PlayerToken.class));
        
        ArgumentCaptor<PlayerToken> tokenCaptor = ArgumentCaptor.forClass(PlayerToken.class);
        verify(playerTokenRepository, times(2)).save(tokenCaptor.capture());
        
        PlayerToken updatedBuyerToken = tokenCaptor.getAllValues().get(1);
        assertThat(updatedBuyerToken.getQuantity()).isEqualTo(15);
    }

    @Test
    void testSell_Success() {
        // Arrange
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(1L)
                .sellerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("60.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                .thenReturn(Optional.of(buyerToken));
        when(playerTokenRepository.findByPlayerAndUser(player, superuser))
                .thenReturn(Optional.of(superuserToken));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        OrderResponse response = orderService.sell(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo("SELL");
        assertThat(response.getQuantity()).isEqualTo(5);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));

        verify(playerTokenRepository, times(2)).save(any(PlayerToken.class));
        verify(userRepository).save(any(User.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testSell_InsufficientTokens() {
        // Arrange
        buyerToken.setQuantity(2);
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(1L)
                .sellerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("60.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                .thenReturn(Optional.of(buyerToken));

        // Act & Assert
        assertThatThrownBy(() -> orderService.sell(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient tokens");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testSell_InvalidQuantity() {
        // Arrange
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(1L)
                .sellerId(1L)
                .quantity(null)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> orderService.sell(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity must be positive");

        verify(playerRepository, never()).findById(any());
    }

    @Test
    void testSell_NoTokens() {
        // Arrange
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(1L)
                .sellerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("60.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.sell(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("don't own any tokens");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testSell_CreateNewSuperuserToken() {
        // Arrange
        SellOrderRequest request = SellOrderRequest.builder()
                .playerId(1L)
                .sellerId(1L)
                .quantity(5)
                .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("60.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                .thenReturn(Optional.of(buyerToken));
        when(playerTokenRepository.findByPlayerAndUser(player, superuser))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        OrderResponse response = orderService.sell(request);

        // Assert
        assertThat(response.getQuantity()).isEqualTo(5);
        verify(playerTokenRepository, times(2)).save(any(PlayerToken.class));
    }

    @Test
    void testCalculateNewAvgPrice() {
        // Arrange
        BuyOrderRequest request = BuyOrderRequest.builder()
                .playerId(1L)
                .buyerId(1L)
                .quantity(5)
                .build();

        buyerToken.setQuantity(10);
        buyerToken.setAvgBuyPrice(new BigDecimal("40.00"));

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
        when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
        when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                .thenReturn(Optional.of(superuserToken));
        when(playerTokenRepository.findByPlayerAndUser(player, buyer))
                .thenReturn(Optional.of(buyerToken));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        orderService.buy(request);

        // Assert - verify the average price was calculated correctly
        ArgumentCaptor<PlayerToken> tokenCaptor = ArgumentCaptor.forClass(PlayerToken.class);
        verify(playerTokenRepository, times(2)).save(tokenCaptor.capture());
        
        PlayerToken updatedToken = tokenCaptor.getAllValues().get(1);
        // (40 * 10 + 50 * 5) / (10 + 5) = (400 + 250) / 15 = 43.33
        assertThat(updatedToken.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("43.33"));
    }
}
