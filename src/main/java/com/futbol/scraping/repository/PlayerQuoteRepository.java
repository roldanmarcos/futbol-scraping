package com.futbol.scraping.repository;

import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerQuoteRepository extends JpaRepository<PlayerQuote, Long> {
    List<PlayerQuote> findByPlayerOrderByQuoteDateDesc(Player player);
    Optional<PlayerQuote> findTopByPlayerOrderByQuoteDateDesc(Player player);

    @Query("SELECT pq FROM PlayerQuote pq WHERE pq.player = :player AND pq.quoteDate <= :date ORDER BY pq.quoteDate DESC")
    List<PlayerQuote> findByPlayerAndDateBefore(@Param("player") Player player, @Param("date") LocalDateTime date);

    @Query("SELECT pq FROM PlayerQuote pq WHERE pq.player.id = :playerId ORDER BY pq.quoteDate DESC")
    List<PlayerQuote> findByPlayerId(@Param("playerId") Long playerId);
}
