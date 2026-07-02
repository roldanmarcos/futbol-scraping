package com.futbol.scraping.model;

import com.futbol.scraping.annotation.FutbolUnit;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class TradeOrderTest {

    @Test
    void onCreateShouldSetCreatedAtAndDefaults() {
        TradeOrder order = TradeOrder.builder()
                .user(User.builder().id(1L).build())
                .player(Player.builder().id(1L).build())
                .orderType(TradeOrder.OrderType.BUY)
                .quantity(10)
                .build();

        order.onCreate();

        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getFilledQuantity()).isZero();
        assertThat(order.getStatus()).isEqualTo(TradeOrder.OrderStatus.PENDING);
    }

    @Test
    void onCreateShouldNotOverrideProvidedValues() {
        TradeOrder order = TradeOrder.builder()
                .user(User.builder().id(1L).build())
                .player(Player.builder().id(1L).build())
                .orderType(TradeOrder.OrderType.SELL)
                .quantity(5)
                .filledQuantity(2)
                .status(TradeOrder.OrderStatus.PARTIALLY_FILLED)
                .build();

        order.onCreate();

        assertThat(order.getFilledQuantity()).isEqualTo(2);
        assertThat(order.getStatus()).isEqualTo(TradeOrder.OrderStatus.PARTIALLY_FILLED);
    }

    @Test
    void getRemainingQuantity_ShouldSubtractFilledFromQuantity() {
        TradeOrder order = TradeOrder.builder()
                .quantity(10)
                .filledQuantity(3)
                .build();

        assertThat(order.getRemainingQuantity()).isEqualTo(7);
    }

    @Test
    void getRemainingQuantity_WhenFullyFilled() {
        TradeOrder order = TradeOrder.builder()
                .quantity(10)
                .filledQuantity(10)
                .build();

        assertThat(order.getRemainingQuantity()).isZero();
    }

    @Test
    void getRemainingQuantity_WhenZeroQuantity() {
        TradeOrder order = TradeOrder.builder()
                .quantity(0)
                .filledQuantity(0)
                .build();

        assertThat(order.getRemainingQuantity()).isZero();
    }

    @Test
    void orderTypeShouldContainBuyAndSell() {
        assertThat(TradeOrder.OrderType.values())
                .containsExactly(TradeOrder.OrderType.BUY, TradeOrder.OrderType.SELL);
    }

    @Test
    void orderStatusShouldContainAllStates() {
        assertThat(TradeOrder.OrderStatus.values())
                .containsExactly(
                        TradeOrder.OrderStatus.PENDING,
                        TradeOrder.OrderStatus.PARTIALLY_FILLED,
                        TradeOrder.OrderStatus.FILLED,
                        TradeOrder.OrderStatus.CANCELLED);
    }

    @Test
    void onCreateWithNullFilledQuantityShouldDefaultToZero() {
        TradeOrder order = TradeOrder.builder()
                .user(User.builder().id(1L).build())
                .player(Player.builder().id(1L).build())
                .orderType(TradeOrder.OrderType.BUY)
                .quantity(10)
                .filledQuantity(null)
                .status(null)
                .build();

        order.onCreate();

        assertThat(order.getFilledQuantity()).isZero();
        assertThat(order.getStatus()).isEqualTo(TradeOrder.OrderStatus.PENDING);
    }

    @Test
    void builderShouldCreateAllFields() {
        User user = User.builder().id(1L).build();
        Player player = Player.builder().id(1L).build();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        TradeOrder order = TradeOrder.builder()
                .id(1L)
                .user(user)
                .player(player)
                .orderType(TradeOrder.OrderType.SELL)
                .quantity(20)
                .filledQuantity(15)
                .status(TradeOrder.OrderStatus.PARTIALLY_FILLED)
                .createdAt(now)
                .build();

        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getPlayer()).isEqualTo(player);
        assertThat(order.getOrderType()).isEqualTo(TradeOrder.OrderType.SELL);
        assertThat(order.getQuantity()).isEqualTo(20);
        assertThat(order.getFilledQuantity()).isEqualTo(15);
        assertThat(order.getStatus()).isEqualTo(TradeOrder.OrderStatus.PARTIALLY_FILLED);
        assertThat(order.getCreatedAt()).isEqualTo(now);
    }
}
