package com.shishu.open.finance;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinPayableMathTest {

    @Test
    void remainingSubtractsPaid() {
        assertEquals(new BigDecimal("70.00"), FinPayableMath.remaining(
                new BigDecimal("100.00"), new BigDecimal("30.00")));
        assertEquals(new BigDecimal("-40.00"), FinPayableMath.remaining(
                new BigDecimal("-100.00"), new BigDecimal("-60.00")));
    }

    @Test
    void statusTransitionsPositive() {
        assertEquals(FinPayableMath.OPEN, FinPayableMath.statusOf(new BigDecimal("100.00"), BigDecimal.ZERO));
        assertEquals(FinPayableMath.PARTIAL, FinPayableMath.statusOf(
                new BigDecimal("100.00"), new BigDecimal("40.00")));
        assertEquals(FinPayableMath.CLOSED, FinPayableMath.statusOf(
                new BigDecimal("100.00"), new BigDecimal("100.00")));
    }

    @Test
    void statusTransitionsNegative() {
        assertEquals(FinPayableMath.OPEN, FinPayableMath.statusOf(new BigDecimal("-50.00"), BigDecimal.ZERO));
        assertEquals(FinPayableMath.PARTIAL, FinPayableMath.statusOf(
                new BigDecimal("-50.00"), new BigDecimal("-20.00")));
        assertEquals(FinPayableMath.CLOSED, FinPayableMath.statusOf(
                new BigDecimal("-50.00"), new BigDecimal("-50.00")));
    }

    @Test
    void allocWithinRemaining() {
        assertTrue(FinPayableMath.allocWithinRemaining(
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00")));
        assertTrue(FinPayableMath.allocWithinRemaining(
                new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("60.00")));
        assertFalse(FinPayableMath.allocWithinRemaining(
                new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("61.00")));
        assertFalse(FinPayableMath.allocWithinRemaining(
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("-1.00")));
        assertTrue(FinPayableMath.allocWithinRemaining(
                new BigDecimal("-80.00"), BigDecimal.ZERO, new BigDecimal("-80.00")));
        assertTrue(FinPayableMath.allocWithinRemaining(
                new BigDecimal("-80.00"), new BigDecimal("-30.00"), new BigDecimal("-50.00")));
        assertFalse(FinPayableMath.allocWithinRemaining(
                new BigDecimal("-80.00"), new BigDecimal("-30.00"), new BigDecimal("-51.00")));
    }

    @Test
    void dueDateUsesSettleDaysOrCash() {
        LocalDate biz = LocalDate.of(2026, 8, 1);
        assertEquals(LocalDate.of(2026, 8, 31), FinPayableMath.dueDate(biz, 30));
        assertEquals(biz, FinPayableMath.dueDate(biz, null));
        assertEquals(biz, FinPayableMath.dueDate(biz, 0));
        assertEquals(biz, FinPayableMath.dueDate(biz, -1));
        assertNull(FinPayableMath.dueDate(null, 7));
    }

    @Test
    void isDueOnOrAfterDueDate() {
        LocalDate biz = LocalDate.of(2026, 8, 1);
        assertFalse(FinPayableMath.isDue(biz, 30, LocalDate.of(2026, 8, 30)));
        assertTrue(FinPayableMath.isDue(biz, 30, LocalDate.of(2026, 8, 31)));
        assertTrue(FinPayableMath.isDue(biz, 30, LocalDate.of(2026, 9, 1)));
        assertTrue(FinPayableMath.isDue(biz, null, LocalDate.of(2026, 8, 1)));
        assertFalse(FinPayableMath.isDue(null, 7, LocalDate.of(2026, 8, 1)));
    }
}
