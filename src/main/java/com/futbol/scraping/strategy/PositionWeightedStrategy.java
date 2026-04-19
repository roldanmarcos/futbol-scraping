package com.futbol.scraping.strategy;

import com.futbol.scraping.model.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Strategy 2: Position-weighted valuation.
 * Adjusts weights based on player's position:
 * - FW: prioritizes goals and shots
 * - MF: prioritizes assists and key passes
 * - DF: prioritizes tackles and appearances
 * - GK: based on appearances
 */
@Component("positionWeighted")
@Slf4j
public class PositionWeightedStrategy implements ValuationStrategy {

    private static final String VERSION = "position-weighted-v1.0";

    @Value("${app.quote.base-value:100.0}")
    private double baseValue;

    @Value("${app.quote.scale-factor:500.0}")
    private double scaleFactor;

    // Weights by position category [goals, assists, shots, keyPasses, tackles, appearances]
    private static final Map<String, double[]> POSITION_WEIGHTS = Map.of(
            "FW",  new double[]{0.35, 0.15, 0.20, 0.10, 0.05, 0.15},
            "MF",  new double[]{0.15, 0.25, 0.10, 0.25, 0.15, 0.10},
            "DF",  new double[]{0.05, 0.05, 0.05, 0.10, 0.40, 0.35},
            "GK",  new double[]{0.00, 0.00, 0.00, 0.05, 0.15, 0.80}
    );

    @Override
    public BigDecimal calculate(Player player) {
        double goals = player.getGoals() != null ? player.getGoals() : 0;
        double assists = player.getAssists() != null ? player.getAssists() : 0;
        double appearances = player.getAppearances() != null ? player.getAppearances() : 1;
        if (appearances == 0) appearances = 1;

        double goalsPerGame = goals / appearances;
        double assistsPerGame = assists / appearances;

        String posCategory = mapPositionCategory(player.getPosition());
        double[] weights = POSITION_WEIGHTS.getOrDefault(posCategory, POSITION_WEIGHTS.get("MF"));

        double nGoals = Math.min(goalsPerGame / 1.0, 1.0);
        double nAssists = Math.min(assistsPerGame / 1.0, 1.0);
        double nShots = Math.min(goalsPerGame * 4.0 / 5.0, 1.0);
        double nKeyPasses = Math.min(assistsPerGame * 3.0 / 3.0, 1.0);
        double nTackles = estimateNormalizedTackles(posCategory);
        double nAppearances = Math.min(appearances / 38.0, 1.0);

        double score = weights[0] * nGoals
                + weights[1] * nAssists
                + weights[2] * nShots
                + weights[3] * nKeyPasses
                + weights[4] * nTackles
                + weights[5] * nAppearances;

        double value = baseValue + (score * scaleFactor);
        log.debug("Player {} ({}) position-weighted score: {}, value: {}", player.getName(), posCategory, score, value);

        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "Position-weighted strategy that adjusts metric weights based on player position (FW/MF/DF/GK)";
    }

    private String mapPositionCategory(String position) {
        if (position == null) return "MF";
        String pos = position.toUpperCase();
        if (pos.contains("FORWARD") || pos.equals("ST") || pos.equals("CF") || pos.equals("SS")
                || pos.equals("LW") || pos.equals("RW") || pos.equals("FW")) {
            return "FW";
        } else if (pos.contains("DEFENDER") || pos.equals("CB") || pos.equals("LB") || pos.equals("RB")
                || pos.equals("DF") || pos.equals("DEF")) {
            return "DF";
        } else if (pos.contains("GOALKEEPER") || pos.equals("GK")) {
            return "GK";
        } else {
            return "MF";
        }
    }

    private double estimateNormalizedTackles(String posCategory) {
        return switch (posCategory) {
            case "DF" -> 0.7;
            case "MF" -> 0.5;
            case "FW" -> 0.1;
            case "GK" -> 0.0;
            default -> 0.3;
        };
    }
}
