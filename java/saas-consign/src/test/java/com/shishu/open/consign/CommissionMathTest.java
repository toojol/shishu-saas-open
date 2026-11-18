package com.shishu.open.consign;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommissionMathTest {

    @Test
    void storeTakeUsesPercentOfSales() {
        assertEquals(new BigDecimal("12.00"),
                CommissionMath.storeTake(new BigDecimal("60.00"), new BigDecimal("20")));
        assertEquals(new BigDecimal("1.50"),
                CommissionMath.storeTake(new BigDecimal("10.00"), new BigDecimal("15")));
    }

    @Test
    void validRateAcceptsZeroToHundred() {
        assertTrue(CommissionMath.validRate(BigDecimal.ZERO));
        assertTrue(CommissionMath.validRate(new BigDecimal("100.00")));
        assertFalse(CommissionMath.validRate(new BigDecimal("-0.01")));
        assertFalse(CommissionMath.validRate(new BigDecimal("100.01")));
        assertFalse(CommissionMath.validRate(null));
    }

    @Test
    void effectiveRatePrefersProductOverride() {
        assertEquals(new BigDecimal("15.00"),
                CommissionMath.effectiveRate(new BigDecimal("15"), new BigDecimal("20")));
        assertEquals(new BigDecimal("20.00"),
                CommissionMath.effectiveRate(null, new BigDecimal("20")));
        assertEquals(null, CommissionMath.effectiveRate(null, null));
    }
}
