package com.futbol.scraping.repository;

import com.futbol.scraping.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByIsSuperuserTrue();

    @Query("SELECT COALESCE(SUM(u.balance), 0) FROM User u")
    BigDecimal sumBalances();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
