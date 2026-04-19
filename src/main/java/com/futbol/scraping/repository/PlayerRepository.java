package com.futbol.scraping.repository;

import com.futbol.scraping.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {
    Optional<Player> findByWhoscoredId(String whoscoredId);
    boolean existsByWhoscoredId(String whoscoredId);
}
