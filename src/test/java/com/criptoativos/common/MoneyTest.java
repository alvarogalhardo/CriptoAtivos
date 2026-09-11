package com.criptoativos.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void cashRoundsToTwoDecimalPlacesHalfEven() {
        assertThat(Money.cash(new BigDecimal("10.005"))).isEqualByComparingTo("10.00");
        assertThat(Money.cash(new BigDecimal("10.015"))).isEqualByComparingTo("10.02");
        assertThat(Money.cash(new BigDecimal("10.1"))).hasToString("10.10");
    }

    @Test
    void unitsRoundsToEightDecimalPlaces() {
        assertThat(Money.units(new BigDecimal("0.123456785"))).hasToString("0.12345678");
    }

    @Test
    void zeroCashCarriesScaleTwo() {
        assertThat(Money.ZERO_CASH).hasToString("0.00");
    }

    @Test
    void isPositiveRejectsNullZeroAndNegative() {
        assertThat(Money.isPositive(null)).isFalse();
        assertThat(Money.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(Money.isPositive(new BigDecimal("0.00"))).isFalse();
        assertThat(Money.isPositive(new BigDecimal("-0.01"))).isFalse();
        assertThat(Money.isPositive(new BigDecimal("0.00000001"))).isTrue();
    }

    @Test
    void repeatedCashRoundingDoesNotDrift() {
        BigDecimal total = Money.ZERO_CASH;
        for (int i = 0; i < 1000; i++) {
            total = Money.cash(total.add(new BigDecimal("0.10")));
        }
        assertThat(total).isEqualByComparingTo("100.00");
    }
}
