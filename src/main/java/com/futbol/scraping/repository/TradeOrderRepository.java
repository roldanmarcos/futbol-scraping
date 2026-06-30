package com.futbol.scraping.repository;

import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.TradeOrder;
import com.futbol.scraping.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM TradeOrder o WHERE o.id = :id")
    Optional<TradeOrder> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT o FROM TradeOrder o WHERE o.player = :player AND o.orderType = 'SELL' AND o.status IN ('PENDING', 'PARTIALLY_FILLED') AND o.user <> :excludeUser ORDER BY o.createdAt ASC")
    List<TradeOrder> findPendingSellOrders(@Param("player") Player player, @Param("excludeUser") User excludeUser);

    @Query("SELECT o FROM TradeOrder o WHERE o.player = :player AND o.orderType = 'BUY' AND o.status IN ('PENDING', 'PARTIALLY_FILLED') AND o.user <> :excludeUser ORDER BY o.createdAt ASC")
    List<TradeOrder> findPendingBuyOrders(@Param("player") Player player, @Param("excludeUser") User excludeUser);

    List<TradeOrder> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT o FROM TradeOrder o WHERE o.player = :player AND o.orderType = 'SELL' AND o.status IN ('PENDING', 'PARTIALLY_FILLED') ORDER BY o.createdAt ASC")
    List<TradeOrder> findActiveSellOrders(@Param("player") Player player);

    @Query("SELECT o FROM TradeOrder o WHERE o.player = :player AND o.orderType = 'BUY' AND o.status IN ('PENDING', 'PARTIALLY_FILLED') ORDER BY o.createdAt ASC")
    List<TradeOrder> findActiveBuyOrders(@Param("player") Player player);
}
