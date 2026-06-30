package com.futbol.scraping.strategy;

import com.futbol.scraping.model.Player;

import java.math.BigDecimal;

public interface ValuationStrategy {
    BigDecimal calculate(Player player);
    String getVersion();
    String getDescription();
}
