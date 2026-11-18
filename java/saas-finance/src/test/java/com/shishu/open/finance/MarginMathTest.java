package com.shishu.open.finance;

import com.shishu.open.core.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarginMathTest {

    @Test
    void rateIsPercent() {
        assertEquals(new BigDecimal("25.00"),
                MarginMath.rate(new BigDecimal("25.00"), new BigDecimal("100.00")));
        assertEquals(new BigDecimal("-10.00"),
                MarginMath.rate(new BigDecimal("-5.00"), new BigDecimal("50.00")));
    }

    @Test
    void rateNullWhenNoSales() {
        assertNull(MarginMath.rate(Money.zero(), Money.zero()));
        assertNull(MarginMath.rate(new BigDecimal("1.00"), BigDecimal.ZERO));
    }
}
