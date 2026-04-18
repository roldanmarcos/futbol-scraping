package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerStatsDTO {
    private String whoscoredId;
    private String name;
    private String team;
    private String league;
    private String position;
    private String nationality;
    private Integer age;
    private Integer weight;
    private Integer appearances;
    private Integer minutesPlayed;
    private Integer goals;
    private Integer assists;
    private Double rating;
    private Integer shotsPerGame;
    private Integer keyPassesPerGame;
    private Integer dribblesPerGame;
    private Integer tacklesPerGame;
    private Double yellowCards;
    private Double redCards;
    private String url;
}
