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
class PerformanceBasedStrategyTest {

    private PerformanceBasedStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PerformanceBasedStrategy();
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
    void calculate_WithNullPosition() {
        Player player = Player.builder()
                .name("Test")
                .goals(10)
                .assists(5)
                .appearances(20)
                .position(null)
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isNotNull();
        assertThat(result).isGreaterThan(BigDecimal.ZERO);
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
        assertThat(result).isGreaterThan(BigDecimal.valueOf(100));
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
        assertThat(result).isGreaterThan(BigDecimal.ZERO);
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
    void calculate_SpecificPositionShortCodes() {
        Player st = Player.builder().name("ST Player").goals(15).assists(5).appearances(30).position("ST").build();
        Player cf = Player.builder().name("CF Player").goals(15).assists(5).appearances(30).position("CF").build();
        Player ss = Player.builder().name("SS Player").goals(15).assists(5).appearances(30).position("SS").build();
        Player am = Player.builder().name("AM Player").goals(15).assists(5).appearances(30).position("AM").build();
        Player cm = Player.builder().name("CM Player").goals(15).assists(5).appearances(30).position("CM").build();
        Player dm = Player.builder().name("DM Player").goals(15).assists(5).appearances(30).position("DM").build();

        assertThat(strategy.calculate(st)).isNotNull();
        assertThat(strategy.calculate(cf)).isNotNull();
        assertThat(strategy.calculate(ss)).isNotNull();
        assertThat(strategy.calculate(am)).isNotNull();
        assertThat(strategy.calculate(cm)).isNotNull();
        assertThat(strategy.calculate(dm)).isNotNull();
    }

    @Test
    void getVersion_ReturnsVersion() {
        assertThat(strategy.getVersion()).isEqualTo("performance-v1.0");
    }

    @Test
    void getDescription_ReturnsDescription() {
        assertThat(strategy.getDescription()).isNotBlank();
    }

    @Test
    void calculate_HighPerformingPlayer() {
        Player player = Player.builder()
                .name("Star")
                .goals(40)
                .assists(20)
                .appearances(38)
                .position("FW")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isGreaterThan(BigDecimal.valueOf(400));
    }

    @Test
    void calculate_LowPerformingPlayer() {
        Player player = Player.builder()
                .name("LowPerformer")
                .goals(0)
                .assists(0)
                .appearances(1)
                .position("DF")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isGreaterThan(BigDecimal.valueOf(100));
    }

    @Test
    void calculate_WithAllStatsMaxedOut() {
        Player player = Player.builder()
                .name("Maxed")
                .goals(100)
                .assists(100)
                .appearances(38)
                .position("FW")
                .build();

        BigDecimal result = strategy.calculate(player);

        assertThat(result).isLessThan(BigDecimal.valueOf(700));
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
    void calculate_DefenderAndCenterBackSameResult() {
        Player cb = Player.builder().name("CB").goals(10).assists(5).appearances(20).position("CB").build();
        Player lb = Player.builder().name("LB").goals(10).assists(5).appearances(20).position("LB").build();

        assertThat(strategy.calculate(cb)).isEqualByComparingTo(strategy.calculate(lb));
    }

    @Test
    void calculate_DefenderPositionVariants() {
        Player rb = Player.builder().name("RB").goals(5).assists(10).appearances(25).position("RB").build();
        Player lb = Player.builder().name("LB").goals(5).assists(10).appearances(25).position("LB").build();

        assertThat(strategy.calculate(rb)).isEqualByComparingTo(strategy.calculate(lb));
    }

    @Test
    void calculate_ForwardPositionDefaults() {
        Player ss = Player.builder().name("SS").goals(5).assists(10).appearances(25).position("SS").build();

        assertThat(strategy.calculate(ss)).isNotNull();
    }

    @Test
    void calculate_DefenderVariants() {
        Player cb = Player.builder().name("CB").goals(1).assists(0).appearances(30).position("CB").build();
        Player lb = Player.builder().name("LB").goals(1).assists(0).appearances(30).position("LB").build();
        Player rb = Player.builder().name("RB").goals(1).assists(0).appearances(30).position("RB").build();
        Player def = Player.builder().name("DEF").goals(1).assists(0).appearances(30).position("DEFENDER").build();

        BigDecimal cbResult = strategy.calculate(cb);
        assertThat(strategy.calculate(lb)).isEqualByComparingTo(cbResult);
        assertThat(strategy.calculate(rb)).isEqualByComparingTo(cbResult);
        assertThat(strategy.calculate(def)).isEqualByComparingTo(cbResult);
    }
}
