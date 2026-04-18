package com.futbol.scraping.strategy;

import com.futbol.scraping.model.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Strategy 1: General performance-based valuation.
 * score = 0.25*goals + 0.15*assists + 0.10*shots + 0.10*keyPasses
 *       + 0.10*dribbles + 0.10*tackles + 0.20*rating
 * value = baseValue + (score * scaleFactor)
 */
@Component("performanceBased")
@Slf4j
public class PerformanceBasedStrategy implements ValuationStrategy {

    private static final String VERSION = "performance-v1.0";

    @Value("${app.quote.base-value:100.0}")
    private double baseValue;

    @Value("${app.quote.scale-factor:500.0}")
    private double scaleFactor;

    @Override
    public BigDecimal calculate(Player player) {
        double goals = player.getGoals() != null ? player.getGoals() : 0;
        double assists = player.getAssists() != null ? player.getAssists() : 0;
        double appearances = player.getAppearances() != null ? player.getAppearances() : 1;
        if (appearances == 0) appearances = 1;

        double goalsPerGame = goals / appearances;
        double assistsPerGame = assists / appearances;

        double shotsPerGame = estimateShotsPerGame(player.getPosition(), goalsPerGame);
        double keyPassesPerGame = estimateKeyPassesPerGame(player.getPosition(), assistsPerGame);
        double dribblesPerGame = estimateDribblesPerGame(player.getPosition());
        double tacklesPerGame = estimateTacklesPerGame(player.getPosition());
        double rating = estimateRating(goalsPerGame, assistsPerGame);

        double nGoals = Math.min(goalsPerGame / 1.0, 1.0);
        double nAssists = Math.min(assistsPerGame / 1.0, 1.0);
        double nShots = Math.min(shotsPerGame / 5.0, 1.0);
        double nKeyPasses = Math.min(keyPassesPerGame / 3.0, 1.0);
        double nDribbles = Math.min(dribblesPerGame / 3.0, 1.0);
        double nTackles = Math.min(tacklesPerGame / 5.0, 1.0);
        double nRating = Math.min(rating / 10.0, 1.0);

        double score = 0.25 * nGoals
                + 0.15 * nAssists
                + 0.10 * nShots
                + 0.10 * nKeyPasses
                + 0.10 * nDribbles
                + 0.10 * nTackles
                + 0.20 * nRating;

        double value = baseValue + (score * scaleFactor);
        log.debug("Player {} performance score: {}, value: {}", player.getName(), score, value);

        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "General performance-based strategy using goals, assists, shots, key passes, dribbles, tackles and rating";
    }

    private double estimateShotsPerGame(String position, double goalsPerGame) {
        if (position == null) return 1.5;
        return switch (position.toUpperCase()) {
            case "FW", "FORWARD", "ST", "CF", "SS" -> goalsPerGame * 4.0 + 2.0;
            case "MF", "MIDFIELDER", "AM", "CM", "DM" -> goalsPerGame * 3.0 + 1.0;
            default -> goalsPerGame * 2.0 + 0.5;
        };
    }

    private double estimateKeyPassesPerGame(String position, double assistsPerGame) {
        if (position == null) return 1.0;
        return switch (position.toUpperCase()) {
            case "MF", "MIDFIELDER", "AM", "CM" -> assistsPerGame * 3.0 + 1.5;
            case "FW", "FORWARD", "ST" -> assistsPerGame * 2.0 + 0.5;
            default -> assistsPerGame * 1.5 + 0.5;
        };
    }

    private double estimateDribblesPerGame(String position) {
        if (position == null) return 1.0;
        return switch (position.toUpperCase()) {
            case "FW", "FORWARD", "ST", "CF" -> 2.0;
            case "MF", "MIDFIELDER", "AM" -> 1.5;
            case "DF", "DEFENDER", "CB", "LB", "RB" -> 0.8;
            default -> 1.0;
        };
    }

    private double estimateTacklesPerGame(String position) {
        if (position == null) return 1.5;
        return switch (position.toUpperCase()) {
            case "DF", "DEFENDER", "CB", "LB", "RB" -> 3.5;
            case "MF", "MIDFIELDER", "DM", "CM" -> 2.5;
            case "FW", "FORWARD" -> 0.5;
            default -> 1.5;
        };
    }

    private double estimateRating(double goalsPerGame, double assistsPerGame) {
        double base = 6.5;
        double bonus = goalsPerGame * 1.0 + assistsPerGame * 0.5;
        return Math.min(base + bonus, 10.0);
    }
}
