package com.futbol.scraping;

import com.futbol.scraping.dto.PortfolioDTO;
import com.futbol.scraping.dto.PortfolioItemDTO;
import com.futbol.scraping.dto.TransactionDTO;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.*;
import com.futbol.scraping.repository.PlayerTokenRepository;
import com.futbol.scraping.repository.TransactionRepository;
import com.futbol.scraping.repository.UserRepository;
import com.futbol.scraping.service.QuoteService;
import com.futbol.scraping.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerTokenRepository playerTokenRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Player testPlayer;
    private PlayerToken testToken;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .balance(new BigDecimal("1000.00"))
                .build();

        testPlayer = Player.builder()
                .id(1L)
                .name("Cristiano Ronaldo")
                .league("Serie A")
                .team("Juventus")
                .position("ST")
                .build();

        testToken = PlayerToken.builder()
                .id(1L)
                .player(testPlayer)
                .user(testUser)
                .quantity(10)
                .avgBuyPrice(new BigDecimal("50.00"))
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .user(testUser)
                .player(testPlayer)
                .transactionType(Transaction.TransactionType.BUY)
                .quantity(5)
                .pricePerToken(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetPortfolio_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(playerTokenRepository.findByUser(testUser)).thenReturn(List.of(testToken));
        when(quoteService.getCurrentPrice(testPlayer)).thenReturn(new BigDecimal("60.00"));

        // Act
        PortfolioDTO portfolio = userService.getPortfolio(1L);

        // Assert
        assertThat(portfolio).isNotNull();
        assertThat(portfolio.getUserId()).isEqualTo(1L);
        assertThat(portfolio.getUsername()).isEqualTo("testuser");
        assertThat(portfolio.getPositions()).hasSize(1);
        assertThat(portfolio.getCurrentValue()).isGreaterThan(BigDecimal.ZERO);
        verify(userRepository).findById(1L);
        verify(playerTokenRepository).findByUser(testUser);
        verify(quoteService).getCurrentPrice(testPlayer);
    }

    @Test
    void testGetPortfolio_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getPortfolio(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(userRepository).findById(999L);
    }

    @Test
    void testGetPortfolio_EmptyTokens() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(playerTokenRepository.findByUser(testUser)).thenReturn(Collections.emptyList());

        // Act
        PortfolioDTO portfolio = userService.getPortfolio(1L);

        // Assert
        assertThat(portfolio).isNotNull();
        assertThat(portfolio.getPositions()).isEmpty();
        assertThat(portfolio.getTotalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testGetPortfolio_FilterZeroQuantityTokens() {
        // Arrange
        PlayerToken zeroToken = PlayerToken.builder()
                .id(2L)
                .player(testPlayer)
                .user(testUser)
                .quantity(0)
                .avgBuyPrice(new BigDecimal("50.00"))
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(playerTokenRepository.findByUser(testUser)).thenReturn(List.of(testToken, zeroToken));
        when(quoteService.getCurrentPrice(testPlayer)).thenReturn(new BigDecimal("60.00"));

        // Act
        PortfolioDTO portfolio = userService.getPortfolio(1L);

        // Assert
        assertThat(portfolio.getPositions()).hasSize(1);
    }

    @Test
    void testGetTransactions_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(testTransaction));

        // Act
        List<TransactionDTO> transactions = userService.getTransactions(1L);

        // Assert
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getId()).isEqualTo(1L);
        assertThat(transactions.get(0).getPlayerName()).isEqualTo("Cristiano Ronaldo");
        assertThat(transactions.get(0).getType()).isEqualTo(Transaction.TransactionType.BUY);
        verify(userRepository).findById(1L);
        verify(transactionRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void testGetTransactions_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getTransactions(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetTransactions_Empty() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(Collections.emptyList());

        // Act
        List<TransactionDTO> transactions = userService.getTransactions(1L);

        // Assert
        assertThat(transactions).isEmpty();
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        User newUser = User.builder()
                .id(2L)
                .username("newuser")
                .email("new@example.com")
                .balance(new BigDecimal("500.00"))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        // Act
        User result = userService.createUser("newuser", "new@example.com", new BigDecimal("500.00"));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).existsByUsername("newuser");
        verify(passwordEncoder).encode(any(String.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser("testuser", "test@example.com", new BigDecimal("500.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already exists");
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }
}
