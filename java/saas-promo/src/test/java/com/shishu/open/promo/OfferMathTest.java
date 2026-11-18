package com.shishu.open.promo;

import com.shishu.open.core.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferMathTest {

    @Test
    void picksThresholdHit() {
        OfferMath.Pick pick = OfferMath.bestMinus(
                List.of(rule(1L, "满100减10", "100", "10", false)),
                new BigDecimal("100.00"),
                false);
        assertTrue(pick.present());
        assertEquals(new BigDecimal("10.00"), pick.amount());
    }

    @Test
    void missesBelowThreshold() {
        OfferMath.Pick pick = OfferMath.bestMinus(
                List.of(rule(1L, "满100减10", "100", "10", false)),
                new BigDecimal("99.99"),
                false);
        assertFalse(pick.present());
    }

    @Test
    void equalMinusPrefersHigherThreshold() {
        OfferMath.Pick pick = OfferMath.bestMinus(
                List.of(
                        rule(1L, "满50减10", "50", "10", false),
                        rule(2L, "满80减10", "80", "10", false)),
                new BigDecimal("100.00"),
                false);
        assertEquals(2L, pick.id());
    }

    @Test
    void prefersLargerMinus() {
        OfferMath.Pick pick = OfferMath.bestMinus(
                List.of(
                        rule(1L, "满50减5", "50", "5", false),
                        rule(2L, "满100减12", "100", "12", false)),
                new BigDecimal("100.00"),
                false);
        assertEquals(2L, pick.id());
        assertEquals(new BigDecimal("12.00"), pick.amount());
    }

    @Test
    void memberOnlyNeedsMember() {
        List<OfferMath.Rule> rules = List.of(rule(1L, "会员满50减8", "50", "8", true));
        assertFalse(OfferMath.bestMinus(rules, new BigDecimal("80.00"), false).present());
        assertTrue(OfferMath.bestMinus(rules, new BigDecimal("80.00"), true).present());
    }

    @Test
    void capsToRemaining() {
        assertEquals(new BigDecimal("15.00"), OfferMath.cap(new BigDecimal("20.00"), new BigDecimal("15.00")));
        assertEquals(Money.zero(), OfferMath.cap(new BigDecimal("10.00"), Money.zero()));
    }

    @Test
    void includeOnlyCountsMatchedCategories() {
        List<OfferMath.AmountLine> lines = List.of(
                line(1L, "80.00"),
                line(2L, "40.00"));
        OfferMath.Rule include = new OfferMath.Rule(
                1L, "生鲜满100减10", new BigDecimal("100"), new BigDecimal("10"),
                false, OfferMath.MODE_INCLUDE, Set.of(1L));
        assertFalse(OfferMath.bestMinus(List.of(include), lines, false).present());

        OfferMath.Rule hit = new OfferMath.Rule(
                2L, "生鲜满50减5", new BigDecimal("50"), new BigDecimal("5"),
                false, OfferMath.MODE_INCLUDE, Set.of(1L));
        OfferMath.Pick pick = OfferMath.bestMinus(List.of(hit), lines, false);
        assertTrue(pick.present());
        assertEquals(new BigDecimal("5.00"), pick.amount());
    }

    @Test
    void excludeDropsListedCategoriesFromBase() {
        List<OfferMath.AmountLine> lines = List.of(
                line(1L, "40.00"),
                line(9L, "80.00")); // 烟酒
        OfferMath.Rule exclude = new OfferMath.Rule(
                1L, "满100减10排除烟酒", new BigDecimal("100"), new BigDecimal("10"),
                false, OfferMath.MODE_EXCLUDE, Set.of(9L));
        // 适用基数 = 40，未达门槛
        assertFalse(OfferMath.bestMinus(List.of(exclude), lines, false).present());

        OfferMath.Rule excludeHit = new OfferMath.Rule(
                2L, "满30减3排除烟酒", new BigDecimal("30"), new BigDecimal("3"),
                false, OfferMath.MODE_EXCLUDE, Set.of(9L));
        OfferMath.Pick pick = OfferMath.bestMinus(List.of(excludeHit), lines, false);
        assertTrue(pick.present());
        assertEquals(new BigDecimal("3.00"), pick.amount());
    }

    @Test
    void allModeUsesFullSubtotal() {
        List<OfferMath.AmountLine> lines = List.of(
                line(1L, "60.00"),
                line(9L, "50.00"));
        OfferMath.Rule all = new OfferMath.Rule(
                1L, "全场满100减10", new BigDecimal("100"), new BigDecimal("10"),
                false, OfferMath.MODE_ALL, Set.of());
        OfferMath.Pick pick = OfferMath.bestMinus(List.of(all), lines, false);
        assertTrue(pick.present());
        assertEquals(new BigDecimal("10.00"), pick.amount());
    }

    @Test
    void eligibleBaseModes() {
        List<OfferMath.AmountLine> lines = List.of(
                line(1L, "30.00"),
                line(2L, "20.00"));
        assertEquals(new BigDecimal("50.00"), OfferMath.eligibleBase(lines, OfferMath.MODE_ALL, Set.of()));
        assertEquals(new BigDecimal("30.00"), OfferMath.eligibleBase(lines, OfferMath.MODE_INCLUDE, Set.of(1L)));
        assertEquals(new BigDecimal("20.00"), OfferMath.eligibleBase(lines, OfferMath.MODE_EXCLUDE, Set.of(1L)));
    }

    private static OfferMath.Rule rule(Long id, String name, String threshold, String minus, boolean memberOnly) {
        return new OfferMath.Rule(id, name, new BigDecimal(threshold), new BigDecimal(minus), memberOnly);
    }

    private static OfferMath.AmountLine line(Long categoryId, String amount) {
        return new OfferMath.AmountLine(categoryId, new BigDecimal(amount));
    }
}
