package com.futbol.scraping.service;

import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.*;
import com.futbol.scraping.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final PlayerTokenRepository playerTokenRepository;
    private final TransactionRepository transactionRepository;
    private final QuoteService quoteService;

    @Transactional
    public OrderResponse buy(BuyOrderRequest request) {
        log.info("Processing BUY order: player={}, buyer={}, qty={}",
                request.getPlayerId(), request.getBuyerId(), request.getQuantity());

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("Quantity must be positive");
        }

        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + request.getPlayerId()));

        User buyer = userRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getBuyerId()));

        User superuser = userRepository.findByIsSuperuserTrue()
                .orElseThrow(() -> new BusinessException("Superuser not found"));

        BigDecimal price = quoteService.getCurrentPrice(player);

        PlayerToken superuserToken = playerTokenRepository
                .findByPlayerAndUserWithLock(player, superuser)
                .orElseThrow(() -> new BusinessException("No tokens available for player: " + player.getName()));

        if (superuserToken.getQuantity() < request.getQuantity()) {
            throw new BusinessException("Not enough tokens available. Available: " + superuserToken.getQuantity());
        }

        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(request.getQuantity()));
        if (buyer.getBalance().compareTo(totalCost) < 0) {
            throw new BusinessException("Insufficient balance. Required: " + totalCost + ", Available: " + buyer.getBalance());
        }

        superuserToken.setQuantity(superuserToken.getQuantity() - request.getQuantity());
        playerTokenRepository.save(superuserToken);

        Optional<PlayerToken> buyerTokenOpt = playerTokenRepository.findByPlayerAndUser(player, buyer);
        if (buyerTokenOpt.isPresent()) {
            PlayerToken buyerToken = buyerTokenOpt.get();
            BigDecimal newAvgPrice = calculateNewAvgPrice(
                    buyerToken.getAvgBuyPrice(), buyerToken.getQuantity(),
                    price, request.getQuantity());
            buyerToken.setQuantity(buyerToken.getQuantity() + request.getQuantity());
            buyerToken.setAvgBuyPrice(newAvgPrice);
            playerTokenRepository.save(buyerToken);
        } else {
            PlayerToken newToken = PlayerToken.builder()
                    .player(player)
                    .user(buyer)
                    .quantity(request.getQuantity())
                    .avgBuyPrice(price)
                    .build();
            playerTokenRepository.save(newToken);
        }

        buyer.setBalance(buyer.getBalance().subtract(totalCost));
        userRepository.save(buyer);

        Transaction transaction = Transaction.builder()
                .user(buyer)
                .player(player)
                .transactionType(Transaction.TransactionType.BUY)
                .quantity(request.getQuantity())
                .pricePerToken(price)
                .totalAmount(totalCost)
                .build();
        Transaction saved = transactionRepository.save(transaction);

        log.info("BUY order completed: transactionId={}, player={}, qty={}, total={}",
                saved.getId(), player.getName(), request.getQuantity(), totalCost);

        return OrderResponse.builder()
                .transactionId(saved.getId())
                .playerId(player.getId())
                .playerName(player.getName())
                .type("BUY")
                .quantity(request.getQuantity())
                .pricePerToken(price)
                .totalAmount(totalCost)
                .timestamp(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public OrderResponse sell(SellOrderRequest request) {
        log.info("Processing SELL order: player={}, seller={}, qty={}",
                request.getPlayerId(), request.getSellerId(), request.getQuantity());

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException("Quantity must be positive");
        }

        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + request.getPlayerId()));

        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getSellerId()));

        User superuser = userRepository.findByIsSuperuserTrue()
                .orElseThrow(() -> new BusinessException("Superuser not found"));

        BigDecimal price = quoteService.getCurrentPrice(player);
        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(request.getQuantity()));

        PlayerToken sellerToken = playerTokenRepository
                .findByPlayerAndUserWithLock(player, seller)
                .orElseThrow(() -> new BusinessException("You don't own any tokens for player: " + player.getName()));

        if (sellerToken.getQuantity() < request.getQuantity()) {
            throw new BusinessException("Insufficient tokens. You own: " + sellerToken.getQuantity());
        }

        sellerToken.setQuantity(sellerToken.getQuantity() - request.getQuantity());
        playerTokenRepository.save(sellerToken);

        Optional<PlayerToken> superuserTokenOpt = playerTokenRepository.findByPlayerAndUser(player, superuser);
        if (superuserTokenOpt.isPresent()) {
            PlayerToken superuserToken = superuserTokenOpt.get();
            superuserToken.setQuantity(superuserToken.getQuantity() + request.getQuantity());
            playerTokenRepository.save(superuserToken);
        } else {
            PlayerToken newToken = PlayerToken.builder()
                    .player(player)
                    .user(superuser)
                    .quantity(request.getQuantity())
                    .avgBuyPrice(BigDecimal.ZERO)
                    .build();
            playerTokenRepository.save(newToken);
        }

        seller.setBalance(seller.getBalance().add(totalAmount));
        userRepository.save(seller);

        Transaction transaction = Transaction.builder()
                .user(seller)
                .player(player)
                .transactionType(Transaction.TransactionType.SELL)
                .quantity(request.getQuantity())
                .pricePerToken(price)
                .totalAmount(totalAmount)
                .build();
        Transaction saved = transactionRepository.save(transaction);

        log.info("SELL order completed: transactionId={}, player={}, qty={}, total={}",
                saved.getId(), player.getName(), request.getQuantity(), totalAmount);

        return OrderResponse.builder()
                .transactionId(saved.getId())
                .playerId(player.getId())
                .playerName(player.getName())
                .type("SELL")
                .quantity(request.getQuantity())
                .pricePerToken(price)
                .totalAmount(totalAmount)
                .timestamp(saved.getCreatedAt())
                .build();
    }

    private BigDecimal calculateNewAvgPrice(BigDecimal currentAvg, int currentQty, BigDecimal newPrice, int newQty) {
        BigDecimal totalCurrent = currentAvg.multiply(BigDecimal.valueOf(currentQty));
        BigDecimal totalNew = newPrice.multiply(BigDecimal.valueOf(newQty));
        return totalCurrent.add(totalNew)
                .divide(BigDecimal.valueOf(currentQty + newQty), 2, RoundingMode.HALF_UP);
    }
}
