package com.futbol.scraping.strategy;

import com.futbol.scraping.annotation.FutbolUnit;
import com.futbol.scraping.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolUnit
class PositionWeightedStrategyTest {

    private PositionWeightedStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PositionWeightedStrategy();
        ReflectionTestUtils.setField(strategy, "baseValue", 100.0);
        ReflectionTestUtils.setField(strategy, "scaleFactor", 500.0);
    }

    @Test
    void calculate_WithNullStats_UsesDefaults() {
        Player player = Player.builder().name("Test").build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
        assertThat(result).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void calculate_NullPositionDefaultsToMidfielder() {
        Player player = Player.builder()
                .name("Test")
                .goals(10)
                .assists(5)
                .appearances(20)
                .position(null)
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
    }

    @Test
    void calculate_ForwardPosition() {
        Player player = Player.builder()
                .name("Forward")
                .goals(20)
                .assists(10)
                .appearances(38)
                .position("FW")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
        assertThat(result).isGreaterThan(BigDecimal.valueOf(100));
    }

    @Test
    void calculate_MidfielderPosition() {
        Player player = Player.builder()
                .name("Midfielder")
                .goals(8)
                .assists(15)
                .appearances(35)
                .position("MF")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
    }

    @Test
    void calculate_DefenderPosition() {
        Player player = Player.builder()
                .name("Defender")
                .goals(2)
                .assists(1)
                .appearances(30)
                .position("DF")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
    }

    @Test
    void calculate_GoalkeeperPosition() {
        Player player = Player.builder()
                .name("Goalkeeper")
                .goals(0)
                .assists(0)
                .appearances(38)
                .position("GK")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
    }

    @Test
    void calculate_WithZeroAppearances() {
        Player player = Player.builder()
                .name("Test")
                .goals(10)
                .assists(5)
                .appearances(0)
                .position("ST")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
    }

    @Test
    void calculate_VariousPositionStrings() {
        Player st = Player.builder().name("ST").goals(15).assists(5).appearances(30).position("ST").build();
        Player cf = Player.builder().name("CF").goals(15).assists(5).appearances(30).position("CF").build();
        Player ss = Player.builder().name("SS").goals(15).assists(5).appearances(30).position("SS").build();
        Player lw = Player.builder().name("LW").goals(15).assists(5).appearances(30).position("LW").build();
        Player rw = Player.builder().name("RW").goals(15).assists(5).appearances(30).position("RW").build();
        Player cb = Player.builder().name("CB").goals(15).assists(5).appearances(30).position("CB").build();
        Player lb = Player.builder().name("LB").goals(15).assists(5).appearances(30).position("LB").build();
        Player rb = Player.builder().name("RB").goals(15).assists(5).appearances(30).position("RB").build();
        Player gk = Player.builder().name("GK").goals(15).assists(5).appearances(30).position("GK").build();
        Player forward = Player.builder().name("FWD").goals(15).assists(5).appearances(30).position("FORWARD").build();
        Player def = Player.builder().name("DEF").goals(15).assists(5).appearances(30).position("DEFENDER").build();
        Player gk2 = Player.builder().name("GK2").goals(15).assists(5).appearances(30).position("GOALKEEPER").build();

        assertThat(strategy.calculate(st)).isNotNull();
        assertThat(strategy.calculate(cf)).isNotNull();
        assertThat(strategy.calculate(ss)).isNotNull();
        assertThat(strategy.calculate(lw)).isNotNull();
        assertThat(strategy.calculate(rw)).isNotNull();
        assertThat(strategy.calculate(cb)).isNotNull();
        assertThat(strategy.calculate(lb)).isNotNull();
        assertThat(strategy.calculate(rb)).isNotNull();
        assertThat(strategy.calculate(gk)).isNotNull();
        assertThat(strategy.calculate(forward)).isNotNull();
        assertThat(strategy.calculate(def)).isNotNull();
        assertThat(strategy.calculate(gk2)).isNotNull();
    }

    @Test
    void calculate_DifferentPositionsGiveDifferentResults() {
        Player fw = Player.builder().name("FW").goals(10).assists(5).appearances(20).position("FW").build();
        Player mf = Player.builder().name("MF").goals(10).assists(5).appearances(20).position("MF").build();
        Player df = Player.builder().name("DF").goals(10).assists(5).appearances(20).position("DF").build();

        assertThat(strategy.calculate(fw)).isNotEqualByComparingTo(strategy.calculate(mf));
        assertThat(strategy.calculate(df)).isNotEqualByComparingTo(strategy.calculate(fw));
    }

    @Test
    void getVersion_ReturnsVersion() {
        assertThat(strategy.getVersion()).isEqualTo("position-weighted-v1.0");
    }

    @Test
    void getDescription_ReturnsDescription() {
        assertThat(strategy.getDescription()).isNotBlank();
    }

    @Test
    void calculate_LowPerformingPlayer() {
        Player player = Player.builder()
                .name("Low")
                .goals(0)
                .assists(0)
                .appearances(1)
                .position("DF")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isGreaterThan(BigDecimal.valueOf(100));
    }

    @Test
    void calculate_HighAppearances() {
        Player player = Player.builder()
                .name("Veteran")
                .goals(5)
                .assists(3)
                .appearances(100)
                .position("DF")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isGreaterThan(BigDecimal.valueOf(100));
    }

    @Test
    void calculate_RoundingToTwoDecimals() {
        Player player = Player.builder()
                .name("Test")
                .goals(3)
                .assists(2)
                .appearances(10)
                .position("MF")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    void calculate_ForwardVariantsSameCategory() {
        Player fw = Player.builder().name("FW").goals(10).assists(5).appearances(20).position("FW").build();
        Player forward = Player.builder().name("FORWARD").goals(10).assists(5).appearances(20).position("FORWARD").build();
        Player st = Player.builder().name("ST").goals(10).assists(5).appearances(20).position("ST").build();

        assertThat(strategy.calculate(fw)).isEqualByComparingTo(strategy.calculate(forward));
        assertThat(strategy.calculate(fw)).isEqualByComparingTo(strategy.calculate(st));
    }

    @Test
    void calculate_DefenderVariantsSameCategory() {
        Player df = Player.builder().name("DF").goals(2).assists(1).appearances(25).position("DF").build();
        Player def = Player.builder().name("DEF").goals(2).assists(1).appearances(25).position("DEF").build();
        Player cb = Player.builder().name("CB").goals(2).assists(1).appearances(25).position("CB").build();

        assertThat(strategy.calculate(df)).isEqualByComparingTo(strategy.calculate(def));
        assertThat(strategy.calculate(df)).isEqualByComparingTo(strategy.calculate(cb));
    }
}
