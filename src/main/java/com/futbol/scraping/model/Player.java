package com.futbol.scraping.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "league")
    private String league;

    @Column(name = "team")
    private String team;

    @Column(name = "position")
    private String position;

    @Column(name = "position_text")
    private String positionText;

    @Column(name = "played_positions")
    private String playedPositions;

    @Column(name = "played_positions_short")
    private String playedPositionsShort;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "team_region_name")
    private String teamRegionName;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "age")
    private Integer age;

    @Column(name = "height")
    private Integer height;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "appearances")
    private Integer appearances;

    @Column(name = "sub_on")
    private Integer subOn;

    @Column(name = "man_of_the_match")
    private Integer manOfTheMatch;

    @Column(name = "goals")
    private Integer goals;

    @Column(name = "assists")
    private Integer assists;

    @Column(name = "minutes_played")
    private Integer minutesPlayed;

    @Column(name = "is_man_of_the_match")
    private Boolean isManOfTheMatch;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "is_opta")
    private Boolean isOpta;

    @Column(name = "tournament_short_name")
    private String tournamentShortName;

    @Column(name = "tournament_id")
    private Long tournamentId;

    @Column(name = "tournament_name")
    private String tournamentName;

    @Column(name = "tournament_region_id")
    private Long tournamentRegionId;

    @Column(name = "tournament_region_code")
    private String tournamentRegionCode;

    @Column(name = "tournament_region_name")
    private String tournamentRegionName;

    @Column(name = "season_id")
    private Long seasonId;

    @Column(name = "season_name")
    private String seasonName;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "shots_per_game")
    private Double shotsPerGame;

    @Column(name = "aerial_won_per_game")
    private Double aerialWonPerGame;

    @Column(name = "yellow_card")
    private Double yellowCard;

    @Column(name = "red_card")
    private Double redCard;

    @Column(name = "pass_success")
    private Double passSuccess;

    @Column(name = "ranking")
    private Integer ranking;

    @Column(name = "player_id", unique = true)
    private Long playerId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "whoscored_id", unique = true)
    private String whoscoredId;

    @Column(name = "url")
    private String url;

    @Column(name = "created_at", updatable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "last_scraped_at", nullable = false)
    private Long lastScrapedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.lastScrapedAt = System.currentTimeMillis();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
