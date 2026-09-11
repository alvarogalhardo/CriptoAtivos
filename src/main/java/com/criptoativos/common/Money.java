package com.criptoativos.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for monetary scale and rounding.
 *
 * <p>The academic version of this project stored balances and quantities as {@code double}. Binary
 * floating point cannot represent values like {@code 0.1} exactly, so repeated arithmetic drifts —
 * unacceptable in anything handling money. Every monetary value in this codebase is a {@link
 * BigDecimal} normalised through this class.
 */
public final class Money {

    /** Cash amounts: currency precision. */
    public static final int CASH_SCALE = 2;

    /** Asset quantities and unit prices: crypto needs far more precision than currency. */
    public static final int UNIT_SCALE = 8;

    /** Banker's rounding — unbiased over many operations, unlike HALF_UP. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static final BigDecimal ZERO_CASH = BigDecimal.ZERO.setScale(CASH_SCALE);

    private Money() {}

    public static BigDecimal cash(BigDecimal value) {
        return value.setScale(CASH_SCALE, ROUNDING);
    }

    public static BigDecimal units(BigDecimal value) {
        return value.setScale(UNIT_SCALE, ROUNDING);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
