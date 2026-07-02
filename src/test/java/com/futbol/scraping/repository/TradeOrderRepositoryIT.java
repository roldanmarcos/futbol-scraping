package com.futbol.scraping.repository;

import com.futbol.scraping.annotation.FutbolJpaIT;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.TradeOrder;
import com.futbol.scraping.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolJpaIT
class TradeOrderRepositoryIT {

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private User buyer;
    private User seller;
    private Player player;

    @BeforeEach
    void setUp() {
        tradeOrderRepository.deleteAll();
        playerRepository.deleteAll();
        userRepository.deleteAll();

        buyer = userRepository.save(User.builder()
                .username("buyer")
                .email("buyer@test.com")
                .passwordHash("h")
                .balance(new BigDecimal("5000"))
                .build());

        seller = userRepository.save(User.builder()
                .username("seller")
                .email("seller@test.com")
                .passwordHash("h")
                .balance(new BigDecimal("3000"))
                .build());

        player = playerRepository.save(Player.builder()
                .name("Messi")
                .league("MLS")
                .team("Inter Miami")
                .position("FW")
                .build());

        tradeOrderRepository.save(TradeOrder.builder()
                .user(seller).player(player)
                .orderType(TradeOrder.OrderType.SELL)
                .quantity(10).filledQuantity(0)
                .status(TradeOrder.OrderStatus.PENDING)
                .build());

        tradeOrderRepository.save(TradeOrder.builder()
                .user(seller).player(player)
                .orderType(TradeOrder.OrderType.SELL)
                .quantity(5).filledQuantity(3)
                .status(TradeOrder.OrderStatus.PARTIALLY_FILLED)
                .build());

        tradeOrderRepository.save(TradeOrder.builder()
                .user(buyer).player(player)
                .orderType(TradeOrder.OrderType.BUY)
                .quantity(8).filledQuantity(0)
                .status(TradeOrder.OrderStatus.PENDING)
                .build());
    }

    @Test
    void findByIdWithLock_returnsOrder() {
        TradeOrder order = tradeOrderRepository.save(TradeOrder.builder()
                .user(seller).player(player)
                .orderType(TradeOrder.OrderType.SELL)
                .quantity(1).filledQuantity(0)
                .status(TradeOrder.OrderStatus.PENDING)
                .build());

        Optional<TradeOrder> found = tradeOrderRepository.findByIdWithLock(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
    }

    @Test
    void findByIdWithLock_withUnknownId_returnsEmpty() {
        assertThat(tradeOrderRepository.findByIdWithLock(99999L)).isEmpty();
    }

    @Test
    void findPendingSellOrders_excludesUser() {
        List<TradeOrder> orders = tradeOrderRepository.findPendingSellOrders(player, buyer);

        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(TradeOrder::getUser)
                .allMatch(u -> !u.getId().equals(buyer.getId()));
    }

    @Test
    void findPendingSellOrders_ordersByCreatedAtAsc() {
        List<TradeOrder> orders = tradeOrderRepository.findPendingSellOrders(player, buyer);

        assertThat(orders.get(0).getCreatedAt()).isBeforeOrEqualTo(orders.get(1).getCreatedAt());
    }

    @Test
    void findPendingBuyOrders_excludesUser() {
        List<TradeOrder> orders = tradeOrderRepository.findPendingBuyOrders(player, seller);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderType()).isEqualTo(TradeOrder.OrderType.BUY);
    }

    @Test
    void findPendingBuyOrders_returnsEmptyForExcludedSeller() {
        List<TradeOrder> orders = tradeOrderRepository.findPendingBuyOrders(player, buyer);

        assertThat(orders).isEmpty();
    }

    @Test
    void findByUserOrderByCreatedAtDesc_returnsUserOrders() {
        List<TradeOrder> sellerOrders = tradeOrderRepository.findByUserOrderByCreatedAtDesc(seller);

        assertThat(sellerOrders).hasSize(2);
        assertThat(sellerOrders).extracting(TradeOrder::getUser)
                .allMatch(u -> u.getId().equals(seller.getId()));
    }

    @Test
    void findByUserOrderByCreatedAtDesc_returnsInDescOrder() {
        List<TradeOrder> sellerOrders = tradeOrderRepository.findByUserOrderByCreatedAtDesc(seller);

        assertThat(sellerOrders.get(0).getCreatedAt())
                .isAfterOrEqualTo(sellerOrders.get(1).getCreatedAt());
    }

    @Test
    void findByUserOrderByCreatedAtDesc_withUserWithNoOrders_returnsEmpty() {
        User emptyUser = userRepository.save(User.builder()
                .username("empty")
                .email("empty@test.com")
                .passwordHash("h")
                .balance(BigDecimal.ZERO)
                .build());

        assertThat(tradeOrderRepository.findByUserOrderByCreatedAtDesc(emptyUser)).isEmpty();
    }

    @Test
    void findActiveSellOrders_returnsPendingAndPartiallyFilled() {
        List<TradeOrder> orders = tradeOrderRepository.findActiveSellOrders(player);

        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(TradeOrder::getOrderType)
                .allMatch(t -> t == TradeOrder.OrderType.SELL);
    }

    @Test
    void findActiveSellOrders_ordersByCreatedAtAsc() {
        List<TradeOrder> orders = tradeOrderRepository.findActiveSellOrders(player);

        assertThat(orders.get(0).getCreatedAt()).isBeforeOrEqualTo(orders.get(1).getCreatedAt());
    }

    @Test
    void findActiveBuyOrders_returnsPendingAndPartiallyFilled() {
        List<TradeOrder> orders = tradeOrderRepository.findActiveBuyOrders(player);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderType()).isEqualTo(TradeOrder.OrderType.BUY);
    }

    @Test
    void findActiveBuyOrders_returnsEmptyForPlayerWithNoBuyOrders() {
        Player otherPlayer = playerRepository.save(Player.builder()
                .name("Ronaldo")
                .league("SPL")
                .team("Al Nassr")
                .position("FW")
                .build());

        assertThat(tradeOrderRepository.findActiveBuyOrders(otherPlayer)).isEmpty();
    }
}
