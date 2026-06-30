package com.futbol.scraping.service;

import com.futbol.scraping.dto.PortfolioDTO;
import com.futbol.scraping.dto.PortfolioItemDTO;
import com.futbol.scraping.dto.TransactionDTO;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.*;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerToken;
import com.futbol.scraping.repository.PlayerRepository;
import com.futbol.scraping.repository.PlayerTokenRepository;
import com.futbol.scraping.repository.TransactionRepository;
import com.futbol.scraping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

        private final UserRepository userRepository;
        private final PlayerRepository playerRepository;
        private final PlayerTokenRepository playerTokenRepository;
        private final TransactionRepository transactionRepository;
        private final QuoteService quoteService;
        private final PasswordEncoder passwordEncoder;

        @Transactional(readOnly = true)
        public Optional<User> findByUsername(String username) {
                return userRepository.findByUsername(username);
        }

        @Transactional
        public User saveUser(User user) {
                return userRepository.save(user);
        }

        @Transactional
        public void allocateTokens(User superuser, int tokensPerPlayer) {
                List<Player> players = playerRepository.findAll();
                int allocated = 0;
                for (Player player : players) {
                        if (playerTokenRepository.findByPlayerAndUser(player, superuser).isEmpty()) {
                                PlayerToken token = PlayerToken.builder()
                                                .player(player)
                                                .user(superuser)
                                                .quantity(tokensPerPlayer)
                                                .avgBuyPrice(BigDecimal.ONE)
                                                .build();
                                playerTokenRepository.save(token);
                                allocated++;
                        }
                }
                if (allocated > 0) {
                        log.info("Allocated {} token positions to superuser", allocated);
                }
        }

        @Transactional(readOnly = true)
        public PortfolioDTO getPortfolio(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

                List<PlayerToken> tokens = playerTokenRepository.findByUser(user);

                BigDecimal totalInvested = BigDecimal.ZERO;
                BigDecimal currentValue = BigDecimal.ZERO;

                List<PortfolioItemDTO> positions = tokens.stream()
                                .filter(t -> t.getQuantity() > 0)
                                .map(t -> {
                                        BigDecimal currentPrice = quoteService.getCurrentPrice(t.getPlayer());
                                        BigDecimal invested = t.getAvgBuyPrice()
                                                        .multiply(BigDecimal.valueOf(t.getQuantity()));
                                        BigDecimal value = currentPrice.multiply(BigDecimal.valueOf(t.getQuantity()));
                                        BigDecimal pl = value.subtract(invested);
                                        BigDecimal plPercent = invested.compareTo(BigDecimal.ZERO) != 0
                                                        ? pl.divide(invested, 4, RoundingMode.HALF_UP)
                                                                        .multiply(BigDecimal.valueOf(100))
                                                        : BigDecimal.ZERO;

                                        return PortfolioItemDTO.builder()
                                                        .playerId(t.getPlayer().getId())
                                                        .playerName(t.getPlayer().getName())
                                                        .league(t.getPlayer().getLeague())
                                                        .team(t.getPlayer().getTeam())
                                                        .position(t.getPlayer().getPosition())
                                                        .quantity(t.getQuantity())
                                                        .avgBuyPrice(t.getAvgBuyPrice())
                                                        .currentPrice(currentPrice)
                                                        .totalInvested(invested)
                                                        .currentValue(value)
                                                        .profitLoss(pl)
                                                        .profitLossPercent(plPercent)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                for (PortfolioItemDTO item : positions) {
                        totalInvested = totalInvested.add(item.getTotalInvested());
                        currentValue = currentValue.add(item.getCurrentValue());
                }

                BigDecimal profitLoss = currentValue.subtract(totalInvested);
                BigDecimal profitLossPercent = totalInvested.compareTo(BigDecimal.ZERO) != 0
                                ? profitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;

                return PortfolioDTO.builder()
                                .userId(userId)
                                .username(user.getUsername())
                                .totalInvested(totalInvested)
                                .currentValue(currentValue)
                                .profitLoss(profitLoss)
                                .profitLossPercent(profitLossPercent)
                                .positions(positions)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<TransactionDTO> getTransactions(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

                return transactionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                                .map(t -> TransactionDTO.builder()
                                                .id(t.getId())
                                                .playerId(t.getPlayer().getId())
                                                .playerName(t.getPlayer().getName())
                                                .type(t.getTransactionType())
                                                .quantity(t.getQuantity())
                                                .pricePerToken(t.getPricePerToken())
                                                .totalAmount(t.getTotalAmount())
                                                .createdAt(t.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Transactional
        public User createUser(String username, String email, BigDecimal initialBalance) {
                return createUser(username, email, UUID.randomUUID().toString(), initialBalance);
        }

        public User createUser(String username, String email, String rawPassword, BigDecimal initialBalance) {
                if (rawPassword == null || rawPassword.isBlank()) {
                        throw new BusinessException("Password is required");
                }
                if (userRepository.existsByUsername(username)) {
                        throw new BusinessException("Username already exists: " + username);
                }
                if (userRepository.existsByEmail(email)) {
                        throw new BusinessException("Email already exists: " + email);
                }
                User user = User.builder()
                                .username(username)
                                .email(email)
                                .passwordHash(passwordEncoder.encode(rawPassword))
                                .balance(initialBalance)
                                .build();
                return userRepository.save(user);
        }
}
