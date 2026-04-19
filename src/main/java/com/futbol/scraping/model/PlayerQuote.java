package com.futbol.scraping.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_quotes", indexes = {
        @Index(name = "idx_player_date", columnList = "player_id,quote_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDateTime quoteDate;

    @Column(name = "strategy_version")
    private String strategyVersion;

    @Column(name = "base_score")
    private BigDecimal baseScore;

    @Column(name = "position_weight")
    private BigDecimal positionWeight;

    @Column(name = "performance_metrics", columnDefinition = "TEXT")
    private String performanceMetrics;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
