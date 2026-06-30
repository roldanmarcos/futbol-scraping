package com.futbol.scraping.repository;

import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerToken;
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
public interface PlayerTokenRepository extends JpaRepository<PlayerToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PlayerToken pt WHERE pt.player = :player AND pt.user = :user")
    Optional<PlayerToken> findByPlayerAndUserWithLock(@Param("player") Player player, @Param("user") User user);

    Optional<PlayerToken> findByPlayerAndUser(Player player, User user);
    List<PlayerToken> findByUser(User user);

    @Query("SELECT COALESCE(SUM(pt.quantity), 0) FROM PlayerToken pt WHERE pt.player = :player")
    Integer sumQuantityByPlayer(@Param("player") Player player);
}
