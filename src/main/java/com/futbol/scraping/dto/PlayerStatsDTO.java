package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerStatsDTO {
    private String whoscoredId;
    private Long playerId;
    private String name;
    private String firstName;
    private String lastName;
    private String team;
    private Long teamId;
    private String league;
    private String position;
    private String positionText;
    private String playedPositions;
    private String playedPositionsShort;
    private String nationality;
    private String teamRegionName;
    private String regionCode;
    private Integer age;
    private Integer height;
    private Integer weight;
    private Integer appearances;
    private Integer subOn;
    private Integer manOfTheMatch;
    private Integer minutesPlayed;
    private Integer goals;
    private Integer assists;
    private Boolean isManOfTheMatch;
    private Boolean isActive;
    private Boolean isOpta;
    private String tournamentShortName;
    private Long tournamentId;
    private String tournamentName;
    private Long tournamentRegionId;
    private String tournamentRegionCode;
    private String tournamentRegionName;
    private Long seasonId;
    private String seasonName;
    private Double rating;
    private Double shotsPerGame;
    private Double aerialWonPerGame;
    private Double yellowCard;
    private Double redCard;
    private Double passSuccess;
    private Integer ranking;
    private String url;
}
