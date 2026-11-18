package com.shishu.open.finance;

import com.shishu.open.finance.DayCloseReconcile.Method;
import com.shishu.open.finance.DayCloseReconcile.Tender;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayCloseReconcileTest {

    @Test
    void balancedWhenTendersMatchSales() {
        var s = DayCloseReconcile.summarize(new BigDecimal("100.00"), List.of(
                new Tender(Method.CASH, new BigDecimal("60.00")),
                new Tender(Method.WECHAT, new BigDecimal("40.00"))));
        assertEquals(new BigDecimal("100.00"), s.tenderTotal());
        assertEquals(new BigDecimal("0.00"), s.unmatched());
        assertTrue(s.balanced());
        assertEquals(new BigDecimal("60.00"), s.byMethod().get(Method.CASH));
        assertEquals(new BigDecimal("0.00"), s.byMethod().get(Method.ALIPAY));
    }

    @Test
    void refundNegativeTenderReducesTotal() {
        var s = DayCloseReconcile.summarize(new BigDecimal("90.00"), List.of(
                new Tender(Method.CASH, new BigDecimal("100.00")),
                new Tender(Method.CASH, new BigDecimal("-10.00"))));
        assertEquals(new BigDecimal("90.00"), s.tenderTotal());
        assertTrue(s.balanced());
    }

    @Test
    void unmatchedWhenTendersShort() {
        var s = DayCloseReconcile.summarize(new BigDecimal("100.00"), List.of(
                new Tender(Method.ALIPAY, new BigDecimal("99.50"))));
        assertEquals(new BigDecimal("0.50"), s.unmatched());
        assertFalse(s.balanced());
    }

    @Test
    void nullTendersAndMethodsIgnored() {
        var s = DayCloseReconcile.summarize(new BigDecimal("10.00"), java.util.Arrays.asList(
                null,
                new Tender(null, new BigDecimal("5.00")),
                new Tender(Method.CASH, new BigDecimal("10.00"))));
        assertEquals(new BigDecimal("10.00"), s.tenderTotal());
        assertTrue(s.balanced());
    }

    @Test
    void cashDiffPositiveIsOverage() {
        assertEquals(new BigDecimal("3.00"),
                DayCloseReconcile.cashDiff(new BigDecimal("500.00"), new BigDecimal("503.00")));
        assertEquals(new BigDecimal("-2.50"),
                DayCloseReconcile.cashDiff(new BigDecimal("500.00"), new BigDecimal("497.50")));
    }
}
