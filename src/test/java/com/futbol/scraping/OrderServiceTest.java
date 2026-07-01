package com.futbol.scraping;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.dto.BuyOrderRequest;
import com.futbol.scraping.dto.OrderResponse;
import com.futbol.scraping.dto.SellOrderRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.*;
import com.futbol.scraping.repository.*;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

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
        private TradeOrderRepository tradeOrderRepository;
        @Mock
        private QuoteService quoteService;

        @Mock
        private MeterRegistry meterRegistry;

        @Mock
        private Counter counter;

        @InjectMocks
        private OrderService orderService;

        private User buyer;
        private User seller;
        private User superuser;
        private Player player;
        private PlayerToken buyerToken;
        private PlayerToken sellerToken;
        private PlayerToken superuserToken;
        private TradeOrder sellOrder;
        private TradeOrder buyOrder;

        @BeforeEach
        void setUp() {
                buyer = User.builder()
                                .id(1L).username("buyer").email("buyer@test.com")
                                .balance(new BigDecimal("1000.00")).isSuperuser(false).build();
                seller = User.builder()
                                .id(3L).username("seller").email("seller@test.com")
                                .balance(new BigDecimal("500.00")).isSuperuser(false).build();
                superuser = User.builder()
                                .id(2L).username("superuser").email("super@test.com")
                                .balance(new BigDecimal("100000.00")).isSuperuser(true).build();
                player = Player.builder()
                                .id(1L).name("Cristiano Ronaldo").league("Serie A")
                                .team("Juventus").position("ST").build();

                buyerToken = PlayerToken.builder()
                                .id(1L).player(player).user(buyer).quantity(10)
                                .avgBuyPrice(new BigDecimal("50.00")).build();
                sellerToken = PlayerToken.builder()
                                .id(2L).player(player).user(seller).quantity(20)
                                .avgBuyPrice(new BigDecimal("40.00")).build();
                superuserToken = PlayerToken.builder()
                                .id(3L).player(player).user(superuser).quantity(100)
                                .avgBuyPrice(BigDecimal.ONE).build();

                sellOrder = TradeOrder.builder()
                                .id(100L).user(seller).player(player)
                                .orderType(TradeOrder.OrderType.SELL)
                                .quantity(20).filledQuantity(0)
                                .status(TradeOrder.OrderStatus.PENDING).build();

                buyOrder = TradeOrder.builder()
                                .id(200L).user(buyer).player(player)
                                .orderType(TradeOrder.OrderType.BUY)
                                .quantity(10).filledQuantity(0)
                                .status(TradeOrder.OrderStatus.PENDING).build();

                ReflectionTestUtils.setField(orderService, "buySuccessCounter", counter);
                ReflectionTestUtils.setField(orderService, "sellSuccessCounter", counter);
        }

        private void mockTradeOrderSave() {
                when(tradeOrderRepository.save(any(TradeOrder.class)))
                                .thenAnswer(i -> {
                                        TradeOrder o = i.getArgument(0);
                                        if (o.getId() == null)
                                                o.setId(ThreadLocalRandom.current().nextLong(1000, 9999));
                                        return o;
                                });
        }

        // ---- BUY ----

        @Test
        void testBuy_MatchesAgainstSellOrders() {
                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(1L).buyerId(1L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
                mockTradeOrderSave();
                when(tradeOrderRepository.findPendingSellOrders(player, buyer))
                                .thenReturn(List.of(sellOrder));
                when(tradeOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sellOrder));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, seller))
                                .thenReturn(Optional.of(sellerToken));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                                .thenReturn(Optional.of(buyerToken));
                when(playerTokenRepository.save(any(PlayerToken.class))).thenAnswer(i -> i.getArgument(0));
                when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

                OrderResponse response = orderService.buy(request);

                assertThat(response.getFilledQuantity()).isEqualTo(5);
                assertThat(response.getRemainingQuantity()).isZero();
                assertThat(response.getStatus()).isEqualTo("FILLED");
                assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
                assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
                assertThat(response.getOrderId()).isNotNull();

                verify(tradeOrderRepository, never()).findPendingBuyOrders(any(), any());
                verify(userRepository, never()).findByIsSuperuserTrue();
        }

        @Test
        void testBuy_FallsBackToSuperuserWhenNoSellOrders() {
                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(1L).buyerId(1L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
                mockTradeOrderSave();
                when(tradeOrderRepository.findPendingSellOrders(player, buyer))
                                .thenReturn(List.of());
                when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                                .thenReturn(Optional.of(superuserToken));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                                .thenReturn(Optional.of(buyerToken));
                when(playerTokenRepository.save(any(PlayerToken.class))).thenAnswer(i -> i.getArgument(0));
                when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

                OrderResponse response = orderService.buy(request);

                assertThat(response.getFilledQuantity()).isEqualTo(5);
                assertThat(response.getRemainingQuantity()).isZero();
                assertThat(response.getStatus()).isEqualTo("FILLED");

                ArgumentCaptor<PlayerToken> captor = ArgumentCaptor.forClass(PlayerToken.class);
                verify(playerTokenRepository, times(2)).save(captor.capture());
                // save #1: superuserToken deduction (quantity 100 -> 95)
                // save #2: buyerToken addition (quantity 10 -> 15, avgBuyPrice updated)
                assertThat(captor.getAllValues().get(0).getQuantity()).isEqualTo(95);
                assertThat(captor.getAllValues().get(1).getQuantity()).isEqualTo(15);
        }

        @Test
        void testBuy_PartialFill_RemainingToSuperuser() {
                buyer.setBalance(new BigDecimal("1500.00"));
                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(1L).buyerId(1L).quantity(25).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
                mockTradeOrderSave();
                when(tradeOrderRepository.findPendingSellOrders(player, buyer))
                                .thenReturn(List.of(sellOrder));
                when(tradeOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sellOrder));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, seller))
                                .thenReturn(Optional.of(sellerToken));
                when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                                .thenReturn(Optional.of(superuserToken));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                                .thenReturn(Optional.of(buyerToken));
                when(playerTokenRepository.save(any(PlayerToken.class))).thenAnswer(i -> i.getArgument(0));
                when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

                OrderResponse response = orderService.buy(request);

                assertThat(response.getFilledQuantity()).isEqualTo(25);
                assertThat(response.getRemainingQuantity()).isZero();
                assertThat(response.getStatus()).isEqualTo("FILLED");
                // 20 from sell order + 5 from superuser
                assertThat(sellerToken.getQuantity()).isZero();
                assertThat(superuserToken.getQuantity()).isEqualTo(95);
                assertThat(buyerToken.getQuantity()).isEqualTo(35);

                verify(transactionRepository, times(3)).save(any(Transaction.class));
        }

        @Test
        void testBuy_InsufficientSuperuserTokens_CreatesPendingOrder() {
                superuserToken.setQuantity(2);
                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(1L).buyerId(1L).quantity(10).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
                mockTradeOrderSave();
                when(tradeOrderRepository.findPendingSellOrders(player, buyer))
                                .thenReturn(List.of());
                when(userRepository.findByIsSuperuserTrue()).thenReturn(Optional.of(superuser));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, superuser))
                                .thenReturn(Optional.of(superuserToken));

                OrderResponse response = orderService.buy(request);

                assertThat(response.getFilledQuantity()).isZero();
                assertThat(response.getRemainingQuantity()).isEqualTo(10);
                assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        @Test
        void testBuy_InsufficientBalance() {
                buyer.setBalance(new BigDecimal("50.00"));
                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(1L).buyerId(1L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("60.00"));

                assertThatThrownBy(() -> orderService.buy(request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("Insufficient balance");

                verify(tradeOrderRepository, never()).save(any());
                verify(transactionRepository, never()).save(any());
        }

        @Test
        void testBuy_InvalidQuantity() {
                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(1L).buyerId(1L).quantity(-5).build();

                assertThatThrownBy(() -> orderService.buy(request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("Quantity must be positive");

                verify(playerRepository, never()).findById(any());
        }

        @Test
        void testBuy_PlayerNotFound() {
                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(999L).buyerId(1L).quantity(5).build();

                when(playerRepository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.buy(request))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Player not found");
        }

        @Test
        void testBuy_UpdatesAvgPrice() {
                buyerToken.setQuantity(10);
                buyerToken.setAvgBuyPrice(new BigDecimal("40.00"));

                BuyOrderRequest request = BuyOrderRequest.builder()
                                .playerId(1L).buyerId(1L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
                mockTradeOrderSave();
                when(tradeOrderRepository.findPendingSellOrders(player, buyer))
                                .thenReturn(List.of(sellOrder));
                when(tradeOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sellOrder));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, seller))
                                .thenReturn(Optional.of(sellerToken));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                                .thenReturn(Optional.of(buyerToken));
                when(playerTokenRepository.save(any(PlayerToken.class))).thenAnswer(i -> i.getArgument(0));
                when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

                orderService.buy(request);

                ArgumentCaptor<PlayerToken> captor = ArgumentCaptor.forClass(PlayerToken.class);
                verify(playerTokenRepository, times(2)).save(captor.capture());
                // First save is sellerToken (deduction), second is buyerToken (addition + avg
                // price update)
                PlayerToken updated = captor.getAllValues().get(1);
                assertThat(updated.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("43.33"));
        }

        // ---- SELL ----

        @Test
        void testSell_MatchesAgainstBuyOrders() {
                SellOrderRequest request = SellOrderRequest.builder()
                                .playerId(1L).sellerId(3L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(3L)).thenReturn(Optional.of(seller));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, seller))
                                .thenReturn(Optional.of(sellerToken));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
                mockTradeOrderSave();
                when(tradeOrderRepository.findPendingBuyOrders(player, seller))
                                .thenReturn(List.of(buyOrder));
                when(tradeOrderRepository.findByIdWithLock(200L)).thenReturn(Optional.of(buyOrder));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, buyer))
                                .thenReturn(Optional.of(buyerToken));
                when(playerTokenRepository.save(any(PlayerToken.class))).thenAnswer(i -> i.getArgument(0));
                when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
                when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

                OrderResponse response = orderService.sell(request);

                assertThat(response.getFilledQuantity()).isEqualTo(5);
                assertThat(response.getRemainingQuantity()).isZero();
                assertThat(response.getStatus()).isEqualTo("FILLED");
                assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
                assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));

                verify(userRepository, never()).findByIsSuperuserTrue();
        }

        @Test
        void testSell_NoBuyOrders_CreatesPendingOrder() {
                SellOrderRequest request = SellOrderRequest.builder()
                                .playerId(1L).sellerId(3L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(3L)).thenReturn(Optional.of(seller));
                when(quoteService.getCurrentPrice(player)).thenReturn(new BigDecimal("50.00"));
                mockTradeOrderSave();
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, seller))
                                .thenReturn(Optional.of(sellerToken));
                when(tradeOrderRepository.findPendingBuyOrders(player, seller))
                                .thenReturn(List.of());

                OrderResponse response = orderService.sell(request);

                assertThat(response.getFilledQuantity()).isZero();
                assertThat(response.getRemainingQuantity()).isEqualTo(5);
                assertThat(response.getStatus()).isEqualTo("PENDING");
        }

        @Test
        void testSell_InsufficientTokens() {
                sellerToken.setQuantity(2);
                SellOrderRequest request = SellOrderRequest.builder()
                                .playerId(1L).sellerId(3L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(3L)).thenReturn(Optional.of(seller));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, seller))
                                .thenReturn(Optional.of(sellerToken));

                assertThatThrownBy(() -> orderService.sell(request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("Insufficient tokens");

                verify(tradeOrderRepository, never()).save(any());
        }

        @Test
        void testSell_InvalidQuantity() {
                SellOrderRequest request = SellOrderRequest.builder()
                                .playerId(1L).sellerId(3L).quantity(null).build();

                assertThatThrownBy(() -> orderService.sell(request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("Quantity must be positive");
        }

        @Test
        void testSell_NoTokens() {
                SellOrderRequest request = SellOrderRequest.builder()
                                .playerId(1L).sellerId(3L).quantity(5).build();

                when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
                when(userRepository.findById(3L)).thenReturn(Optional.of(seller));
                when(playerTokenRepository.findByPlayerAndUserWithLock(player, seller))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.sell(request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("don't own any tokens");

                verify(tradeOrderRepository, never()).save(any());
        }

        // ---- CANCEL ----

        @Test
        void testCancel_OwnOrder() {
                when(tradeOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sellOrder));
                orderService.cancelOrder(100L, 3L);

                assertThat(sellOrder.getStatus()).isEqualTo(TradeOrder.OrderStatus.CANCELLED);
                verify(tradeOrderRepository).save(sellOrder);
        }

        @Test
        void testCancel_NotOwner_Throws() {
                when(tradeOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sellOrder));
                when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
                assertThatThrownBy(() -> orderService.cancelOrder(100L, 1L))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("You can only cancel your own orders");
        }

        @Test
        void testCancel_SuperuserCancelsAnyOrder() {
                when(tradeOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sellOrder));
                when(userRepository.findById(2L)).thenReturn(Optional.of(superuser));
                orderService.cancelOrder(100L, 2L);

                assertThat(sellOrder.getStatus()).isEqualTo(TradeOrder.OrderStatus.CANCELLED);
        }

        @Test
        void testCancel_FilledOrder_Throws() {
                sellOrder.setStatus(TradeOrder.OrderStatus.FILLED);
                when(tradeOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(sellOrder));
                assertThatThrownBy(() -> orderService.cancelOrder(100L, 3L))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("Cannot cancel a filled order");
        }

        @Test
        void testCancel_OrderNotFound() {
                when(tradeOrderRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());
                assertThatThrownBy(() -> orderService.cancelOrder(999L, 1L))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Order not found");
        }
}
