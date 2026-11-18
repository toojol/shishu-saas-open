package com.shishu.open.promo;

import com.shishu.open.core.BizModes;
import com.shishu.open.core.ProductTypes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealMathTest {

    @Test
    void buyGetOneFree() {
        DealMath.Rule rule = qtyRule(1L, DealKinds.BUY_GET, Set.of(10L),
                bd("2"), bd("1"), null, null, null, null, null, null, 0);
        DealMath.Line line = line(10L, 1L, bd("3"), bd("10.00"));
        DealMath.Pick pick = DealMath.bestQtyDeal(List.of(rule), List.of(line), false);
        assertTrue(pick.present());
        assertEquals(bd("10.00"), pick.amount());
    }

    @Test
    void nForM() {
        DealMath.Rule rule = qtyRule(2L, DealKinds.N_FOR_M, Set.of(10L),
                null, null, bd("3"), bd("20.00"), null, null, null, null, 0);
        DealMath.Line line = line(10L, 1L, bd("3"), bd("10.00"));
        assertEquals(bd("10.00"), DealMath.bestQtyDeal(List.of(rule), List.of(line), false).amount());
    }

    @Test
    void secondHalfOff() {
        DealMath.Rule rule = qtyRule(3L, DealKinds.SECOND_OFF, Set.of(10L),
                null, null, null, null, bd("50.00"), null, null, null, 0);
        DealMath.Line line = line(10L, 1L, bd("2"), bd("10.00"));
        assertEquals(bd("5.00"), DealMath.bestQtyDeal(List.of(rule), List.of(line), false).amount());
    }

    @Test
    void nthFixedPoolsAcrossSkusSamePrice() {
        DealMath.Rule rule = qtyRule(4L, DealKinds.NTH_FIXED, Set.of(10L, 11L),
                null, null, null, null, null, bd("2"), bd("1.00"), DealKinds.NTH_MODE_FIXED, 0);
        List<DealMath.Line> lines = List.of(
                line(10L, 1L, bd("1"), bd("8.00")),
                line(11L, 1L, bd("1"), bd("8.00")));
        assertEquals(bd("7.00"), DealMath.bestQtyDeal(List.of(rule), lines, false).amount());
    }

    @Test
    void nthFixedSortsHighPricePaysFull() {
        DealMath.Rule rule = qtyRule(5L, DealKinds.NTH_FIXED, Set.of(10L, 11L),
                null, null, null, null, null, bd("2"), bd("1.00"), DealKinds.NTH_MODE_FIXED, 0);
        List<DealMath.Line> lines = List.of(
                line(10L, 1L, bd("1"), bd("8.00")),
                line(11L, 1L, bd("1"), bd("6.00")));
        assertEquals(bd("5.00"), DealMath.bestQtyDeal(List.of(rule), lines, false).amount());
    }

    @Test
    void nthPercentPoolsAcrossSkus() {
        DealMath.Rule rule = qtyRule(9L, DealKinds.NTH_FIXED, Set.of(10L, 11L),
                null, null, null, null, bd("50.00"), bd("2"), null, DealKinds.NTH_MODE_PERCENT, 0);
        List<DealMath.Line> lines = List.of(
                line(10L, 1L, bd("1"), bd("8.00")),
                line(11L, 1L, bd("1"), bd("6.00")));
        // 排队 [8,6]，第 2 件五折 → 减免 3
        assertEquals(bd("3.00"), DealMath.bestQtyDeal(List.of(rule), lines, false).amount());
    }

    @Test
    void nthFixedEveryThird() {
        DealMath.Rule rule = qtyRule(6L, DealKinds.NTH_FIXED, Set.of(10L),
                null, null, null, null, null, bd("3"), bd("1.00"), DealKinds.NTH_MODE_FIXED, 0);
        DealMath.Line line = line(10L, 1L, bd("3"), bd("10.00"));
        assertEquals(bd("9.00"), DealMath.bestQtyDeal(List.of(rule), List.of(line), false).amount());
    }

    @Test
    void nthFixedNoOffWhenUnitBelowFixed() {
        DealMath.Rule rule = qtyRule(7L, DealKinds.NTH_FIXED, Set.of(10L),
                null, null, null, null, null, bd("2"), bd("5.00"), DealKinds.NTH_MODE_FIXED, 0);
        DealMath.Line line = line(10L, 1L, bd("2"), bd("3.00"));
        assertFalse(DealMath.bestQtyDeal(List.of(rule), List.of(line), false).present());
    }

    @Test
    void nthFixedSinglePieceMisses() {
        DealMath.Rule rule = qtyRule(8L, DealKinds.NTH_FIXED, Set.of(10L, 11L),
                null, null, null, null, null, bd("2"), bd("1.00"), DealKinds.NTH_MODE_FIXED, 0);
        assertFalse(DealMath.bestQtyDeal(List.of(rule), List.of(line(10L, 1L, bd("1"), bd("8.00"))), false).present());
    }

    @Test
    void scopePercentStore() {
        DealMath.Rule rule = new DealMath.Rule(
                5L, "全场九折", DealKinds.STORE_OFF,
                null, null, null, null, null, null, null, null, bd("90.00"),
                false, false, 1, false, Set.of(), Set.of(),
                null, null, null, null, null, null);
        BigDecimal pct = DealMath.scopePercentForProduct(
                List.of(rule), 10L, 1L, ProductTypes.STANDARD, BizModes.PURCHASE, false);
        assertEquals(bd("90.00"), pct);
        assertEquals(bd("9.00"), DealMath.scopeUnitPrice(bd("10.00"), pct));
    }

    @Test
    void scopeExcludesWeigh() {
        DealMath.Rule rule = new DealMath.Rule(
                5L, "全场九折", DealKinds.STORE_OFF,
                null, null, null, null, null, null, null, null, bd("90.00"),
                true, false, 1, false, Set.of(), Set.of(),
                null, null, null, null, null, null);
        assertNull(DealMath.scopePercentForProduct(
                List.of(rule), 10L, 1L, ProductTypes.WEIGH, BizModes.PURCHASE, false));
    }

    @Test
    void pickGiftUsesHighestThreshold() {
        DealMath.Rule low = giftRule(1L, bd("50.00"), 10L, "赠品A", bd("1"), 0);
        DealMath.Rule high = giftRule(2L, bd("100.00"), 20L, "赠品B", bd("2"), 1);
        DealMath.GiftPick pick = DealMath.pickGift(List.of(low, high), bd("120.00"), false);
        assertTrue(pick.present());
        assertEquals(2L, pick.id());
        assertEquals(20L, pick.giftProductId());
        assertEquals(bd("2.000"), pick.giftQty());
    }

    @Test
    void pickGiftRequiresThreshold() {
        DealMath.Rule rule = giftRule(1L, bd("100.00"), 10L, "赠品", bd("1"), 0);
        assertFalse(DealMath.pickGift(List.of(rule), bd("99.99"), false).present());
    }

    private static DealMath.Rule giftRule(
            Long id, BigDecimal threshold, Long productId, String name, BigDecimal qty, int priority) {
        return new DealMath.Rule(
                id, "满额送", DealKinds.THRESHOLD_GIFT,
                null, null, null, null, null, null, null, null, null,
                false, false, priority, false, Set.of(), Set.of(),
                threshold, productId, name, qty, null, null);
    }

    private static DealMath.Rule qtyRule(
            Long id, String kind, Set<Long> products,
            BigDecimal buy, BigDecimal get, BigDecimal nQty, BigDecimal nAmount,
            BigDecimal second, BigDecimal nthQty, BigDecimal nthAmount, String nthMode, int priority) {
        return new DealMath.Rule(
                id, "t", kind, buy, get, nQty, nAmount, second, nthQty, nthAmount, nthMode, null,
                false, false, priority, false, products, Set.of(),
                null, null, null, null, null, null);
    }

    private static DealMath.Line line(Long productId, Long categoryId, BigDecimal qty, BigDecimal unit) {
        return new DealMath.Line(
                productId, categoryId, ProductTypes.STANDARD, BizModes.PURCHASE,
                "RETAIL", qty, unit, unit.multiply(qty));
    }

    private static BigDecimal bd(String raw) {
        return new BigDecimal(raw);
    }
}
