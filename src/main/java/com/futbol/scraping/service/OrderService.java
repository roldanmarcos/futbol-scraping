package com.futbol.scraping.service;

import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderBookResponse;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.*;
import com.futbol.scraping.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final PlayerTokenRepository playerTokenRepository;
    private final TransactionRepository transactionRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final QuoteService quoteService;
    private final MeterRegistry meterRegistry;
    private Counter buySuccessCounter;
    private Counter sellSuccessCounter;

    private Counter buySuccessCounter() {
        if (buySuccessCounter == null) {
            buySuccessCounter = Counter.builder("orders.buy.success")
                    .description("Número de compras de tokens exitosas")
                    .register(meterRegistry);
        }
        return buySuccessCounter;
    }

    private Counter sellSuccessCounter() {
        if (sellSuccessCounter == null) {
            sellSuccessCounter = Counter.builder("orders.sell.success")
                    .description("Número de ventas de tokens exitosas")
                    .register(meterRegistry);
        }
        return sellSuccessCounter;
    }

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

        BigDecimal price = quoteService.getCurrentPrice(player);

        int remainingQty = request.getQuantity();

        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(request.getQuantity()));
        if (buyer.getBalance().compareTo(totalCost) < 0) {
            throw new BusinessException("Insufficient balance. Required: " + totalCost + ", Available: "
                    + buyer.getBalance());
        }

        TradeOrder buyOrder = TradeOrder.builder()
                .user(buyer)
                .player(player)
                .orderType(TradeOrder.OrderType.BUY)
                .quantity(request.getQuantity())
                .filledQuantity(0)
                .status(TradeOrder.OrderStatus.PENDING)
                .build();
        buyOrder = tradeOrderRepository.save(buyOrder);

        int filledQty = 0;

        List<TradeOrder> pendingSells = tradeOrderRepository.findPendingSellOrders(player, buyer);

        for (TradeOrder sellOrder : pendingSells) {
            if (remainingQty <= 0) break;

            TradeOrder lockedSell = tradeOrderRepository.findByIdWithLock(sellOrder.getId())
                    .orElseThrow(() -> new BusinessException("Sell order not found"));

            if (lockedSell.getStatus() == TradeOrder.OrderStatus.FILLED ||
                lockedSell.getStatus() == TradeOrder.OrderStatus.CANCELLED) {
                continue;
            }

            int execQty = Math.min(remainingQty, lockedSell.getRemainingQuantity());
            BigDecimal total = price.multiply(BigDecimal.valueOf(execQty));

            lockedSell.setFilledQuantity(lockedSell.getFilledQuantity() + execQty);
            lockedSell.setStatus(lockedSell.getFilledQuantity() >= lockedSell.getQuantity()
                    ? TradeOrder.OrderStatus.FILLED : TradeOrder.OrderStatus.PARTIALLY_FILLED);
            tradeOrderRepository.save(lockedSell);

            User seller = lockedSell.getUser();
            transferTokens(seller, buyer, player, execQty, price);

            buyer.setBalance(buyer.getBalance().subtract(total));
            userRepository.save(buyer);

            seller.setBalance(seller.getBalance().add(total));
            userRepository.save(seller);

            transactionRepository.save(Transaction.builder()
                    .user(buyer).player(player)
                    .transactionType(Transaction.TransactionType.BUY)
                    .quantity(execQty).pricePerToken(price).totalAmount(total).build());

            transactionRepository.save(Transaction.builder()
                    .user(seller).player(player)
                    .transactionType(Transaction.TransactionType.SELL)
                    .quantity(execQty).pricePerToken(price).totalAmount(total).build());

            filledQty += execQty;
            remainingQty -= execQty;
        }

        if (remainingQty > 0) {
            User superuser = userRepository.findByIsSuperuserTrue()
                    .orElseThrow(() -> new BusinessException("Superuser not found"));

            PlayerToken superuserToken = playerTokenRepository
                    .findByPlayerAndUserWithLock(player, superuser).orElse(null);

            if (superuserToken != null && superuserToken.getQuantity() >= remainingQty) {
                int execQty = remainingQty;
                BigDecimal total = price.multiply(BigDecimal.valueOf(execQty));

                transferTokens(superuser, buyer, player, execQty, price);

                buyer.setBalance(buyer.getBalance().subtract(total));
                userRepository.save(buyer);

                superuser.setBalance(superuser.getBalance().add(total));
                userRepository.save(superuser);

                transactionRepository.save(Transaction.builder()
                        .user(buyer).player(player)
                        .transactionType(Transaction.TransactionType.BUY)
                        .quantity(execQty).pricePerToken(price).totalAmount(total).build());

                filledQty += execQty;
                remainingQty = 0;
            }
        }

        buyOrder.setFilledQuantity(filledQty);
        if (remainingQty == 0) {
            buyOrder.setStatus(TradeOrder.OrderStatus.FILLED);
        } else if (filledQty > 0) {
            buyOrder.setStatus(TradeOrder.OrderStatus.PARTIALLY_FILLED);
        }
        tradeOrderRepository.save(buyOrder);

        BigDecimal totalFilled = price.multiply(BigDecimal.valueOf(filledQty));

        log.info("BUY order processed: orderId={}, filled={}, remaining={}",
                buyOrder.getId(), filledQty, remainingQty);


        buySuccessCounter().increment();

        return OrderResponse.builder()
                .orderId(buyOrder.getId())
                .playerId(player.getId())
                .playerName(player.getName())
                .type("BUY")
                .price(price)
                .quantity(request.getQuantity())
                .filledQuantity(filledQty)
                .remainingQuantity(remainingQty)
                .status(buyOrder.getStatus().name())
                .totalAmount(totalFilled)
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

        PlayerToken sellerToken = playerTokenRepository
                .findByPlayerAndUserWithLock(player, seller)
                .orElseThrow(() -> new BusinessException("You don't own any tokens for player: " + player.getName()));

        if (sellerToken.getQuantity() < request.getQuantity()) {
            throw new BusinessException("Insufficient tokens. You own: " + sellerToken.getQuantity());
        }

        BigDecimal price = quoteService.getCurrentPrice(player);

        int remainingQty = request.getQuantity();

        TradeOrder sellOrder = TradeOrder.builder()
                .user(seller)
                .player(player)
                .orderType(TradeOrder.OrderType.SELL)
                .quantity(request.getQuantity())
                .filledQuantity(0)
                .status(TradeOrder.OrderStatus.PENDING)
                .build();
        sellOrder = tradeOrderRepository.save(sellOrder);

        int filledQty = 0;

        List<TradeOrder> pendingBuys = tradeOrderRepository.findPendingBuyOrders(player, seller);

        for (TradeOrder buyOrder : pendingBuys) {
            if (remainingQty <= 0) break;

            TradeOrder lockedBuy = tradeOrderRepository.findByIdWithLock(buyOrder.getId())
                    .orElseThrow(() -> new BusinessException("Buy order not found"));

            if (lockedBuy.getStatus() == TradeOrder.OrderStatus.FILLED ||
                lockedBuy.getStatus() == TradeOrder.OrderStatus.CANCELLED) {
                continue;
            }

            int execQty = Math.min(remainingQty, lockedBuy.getRemainingQuantity());
            BigDecimal total = price.multiply(BigDecimal.valueOf(execQty));

            lockedBuy.setFilledQuantity(lockedBuy.getFilledQuantity() + execQty);
            lockedBuy.setStatus(lockedBuy.getFilledQuantity() >= lockedBuy.getQuantity()
                    ? TradeOrder.OrderStatus.FILLED : TradeOrder.OrderStatus.PARTIALLY_FILLED);
            tradeOrderRepository.save(lockedBuy);

            User buyer = lockedBuy.getUser();
            transferTokens(seller, buyer, player, execQty, price);

            buyer.setBalance(buyer.getBalance().subtract(total));
            userRepository.save(buyer);

            seller.setBalance(seller.getBalance().add(total));
            userRepository.save(seller);

            transactionRepository.save(Transaction.builder()
                    .user(seller).player(player)
                    .transactionType(Transaction.TransactionType.SELL)
                    .quantity(execQty).pricePerToken(price).totalAmount(total).build());

            transactionRepository.save(Transaction.builder()
                    .user(buyer).player(player)
                    .transactionType(Transaction.TransactionType.BUY)
                    .quantity(execQty).pricePerToken(price).totalAmount(total).build());

            filledQty += execQty;
            remainingQty -= execQty;
        }

        sellOrder.setFilledQuantity(filledQty);
        if (remainingQty == 0) {
            sellOrder.setStatus(TradeOrder.OrderStatus.FILLED);
        } else if (filledQty > 0) {
            sellOrder.setStatus(TradeOrder.OrderStatus.PARTIALLY_FILLED);
        }
        tradeOrderRepository.save(sellOrder);

        BigDecimal totalFilled = price.multiply(BigDecimal.valueOf(filledQty));

        log.info("SELL order processed: orderId={}, filled={}, remaining={}",
                sellOrder.getId(), filledQty, remainingQty);

        sellSuccessCounter().increment();

        return OrderResponse.builder()
                .orderId(sellOrder.getId())
                .playerId(player.getId())
                .playerName(player.getName())
                .type("SELL")
                .price(price)
                .quantity(request.getQuantity())
                .filledQuantity(filledQty)
                .remainingQuantity(remainingQty)
                .status(sellOrder.getStatus().name())
                .totalAmount(totalFilled)
                .build();
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        TradeOrder order = tradeOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            if (!Boolean.TRUE.equals(user.getIsSuperuser())) {
                throw new BusinessException("You can only cancel your own orders");
            }
        }

        if (order.getStatus() == TradeOrder.OrderStatus.FILLED) {
            throw new BusinessException("Cannot cancel a filled order");
        }
        if (order.getStatus() == TradeOrder.OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled");
        }

        order.setStatus(TradeOrder.OrderStatus.CANCELLED);
        tradeOrderRepository.save(order);

        log.info("Order cancelled: orderId={}", orderId);
    }

    public OrderBookResponse getOrderBook(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        BigDecimal price = quoteService.getCurrentPrice(player);

        List<TradeOrder> asks = tradeOrderRepository.findActiveSellOrders(player);
        List<TradeOrder> bids = tradeOrderRepository.findActiveBuyOrders(player);

        int buyQty = bids.stream().mapToInt(TradeOrder::getRemainingQuantity).sum();
        int sellQty = asks.stream().mapToInt(TradeOrder::getRemainingQuantity).sum();

        return OrderBookResponse.builder()
                .playerId(player.getId())
                .playerName(player.getName())
                .price(price)
                .buyQuantity(buyQty)
                .buyOrderCount(bids.size())
                .sellQuantity(sellQty)
                .sellOrderCount(asks.size())
                .build();
    }

    private void transferTokens(User from, User to, Player player, int quantity, BigDecimal price) {
        PlayerToken fromToken = playerTokenRepository
                .findByPlayerAndUserWithLock(player, from)
                .orElseThrow(() -> new BusinessException(from.getUsername() + " no longer has tokens for " + player.getName()));

        fromToken.setQuantity(fromToken.getQuantity() - quantity);
        playerTokenRepository.save(fromToken);

        PlayerToken toToken = playerTokenRepository
                .findByPlayerAndUserWithLock(player, to)
                .orElseGet(() -> PlayerToken.builder()
                        .player(player)
                        .user(to)
                        .quantity(0)
                        .avgBuyPrice(BigDecimal.ZERO)
                        .build());

        BigDecimal newAvgPrice = calculateNewAvgPrice(toToken.getAvgBuyPrice(), toToken.getQuantity(), price, quantity);
        toToken.setQuantity(toToken.getQuantity() + quantity);
        toToken.setAvgBuyPrice(newAvgPrice);
        playerTokenRepository.save(toToken);
    }

    private BigDecimal calculateNewAvgPrice(BigDecimal currentAvg, int currentQty, BigDecimal newPrice, int newQty) {
        if (currentQty == 0) return newPrice;
        BigDecimal bdCurrentQty = BigDecimal.valueOf(currentQty);
        BigDecimal totalCurrent = currentAvg.multiply(bdCurrentQty);
        BigDecimal bdNewQty = BigDecimal.valueOf(newQty);
        BigDecimal totalNew = newPrice.multiply(bdNewQty);
        BigDecimal suma = bdCurrentQty.add(bdNewQty);
        return totalCurrent.add(totalNew)
                .divide(suma, 2, RoundingMode.HALF_UP);
    }
}
