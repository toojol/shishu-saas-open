package com.shishu.open.promo;

import com.shishu.open.core.Money;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/** 满减取最优一档；券抵额不超过剩余应收。 */
public final class OfferMath {

    public static final String MODE_ALL = "ALL";
    public static final String MODE_INCLUDE = "INCLUDE";
    public static final String MODE_EXCLUDE = "EXCLUDE";

    private OfferMath() {
    }

    public record Rule(
            Long id,
            String name,
            BigDecimal threshold,
            BigDecimal minus,
            boolean memberOnly,
            String categoryMode,
            Set<Long> categoryIds) {

        public Rule(Long id, String name, BigDecimal threshold, BigDecimal minus, boolean memberOnly) {
            this(id, name, threshold, minus, memberOnly, MODE_ALL, Set.of());
        }

        public Rule {
            categoryMode = normalizeCategoryMode(categoryMode);
            categoryIds = categoryIds == null || categoryIds.isEmpty()
                    ? Set.of()
                    : Set.copyOf(categoryIds);
        }
    }

    public record AmountLine(Long categoryId, BigDecimal amount) {
    }

    public record Pick(Long id, String name, BigDecimal amount) {
        public static Pick none() {
            return new Pick(null, null, Money.zero());
        }

        public boolean present() {
            return id != null && amount != null && amount.compareTo(Money.zero()) > 0;
        }
    }

    public static String normalizeCategoryMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MODE_ALL;
        }
        String code = mode.trim().toUpperCase(Locale.ROOT);
        return switch (code) {
            case MODE_INCLUDE, MODE_EXCLUDE -> code;
            default -> MODE_ALL;
        };
    }

    /** 按整单小计取最优满减（各规则按 ALL 基数）。 */
    public static Pick bestMinus(List<Rule> rules, BigDecimal subtotal, boolean hasMember) {
        BigDecimal sum = Money.of(subtotal);
        return pickBest(rules, hasMember, rule -> sum);
    }

    /** 按行品类与各规则 categoryMode 收窄基数后取最优满减。 */
    public static Pick bestMinus(List<Rule> rules, Collection<AmountLine> lines, boolean hasMember) {
        return pickBest(rules, hasMember, rule -> eligibleBase(lines, rule.categoryMode(), rule.categoryIds()));
    }

    private static Pick pickBest(List<Rule> rules, boolean hasMember, Function<Rule, BigDecimal> baseOf) {
        Pick best = Pick.none();
        BigDecimal bestMinus = Money.zero();
        BigDecimal bestThreshold = Money.zero();
        if (rules == null) {
            return best;
        }
        for (Rule rule : rules) {
            if (rule == null || rule.id() == null) {
                continue;
            }
            if (rule.memberOnly() && !hasMember) {
                continue;
            }
            BigDecimal sum = Money.of(baseOf.apply(rule));
            if (sum.compareTo(Money.zero()) <= 0) {
                continue;
            }
            BigDecimal threshold = Money.of(rule.threshold());
            if (sum.compareTo(threshold) < 0) {
                continue;
            }
            BigDecimal minus = cap(rule.minus(), sum);
            if (minus.compareTo(Money.zero()) <= 0) {
                continue;
            }
            if (minus.compareTo(bestMinus) > 0
                    || (minus.compareTo(bestMinus) == 0 && threshold.compareTo(bestThreshold) > 0)) {
                bestMinus = minus;
                bestThreshold = threshold;
                best = new Pick(rule.id(), rule.name(), minus);
            }
        }
        return best;
    }

    /**
     * 满减适用基数。ALL 或名单空 → 行合计；INCLUDE → 命中合计；EXCLUDE → 行合计 − 命中合计。
     */
    public static BigDecimal eligibleBase(Collection<AmountLine> lines, String mode, Set<Long> cats) {
        BigDecimal all = sumAll(lines);
        String m = normalizeCategoryMode(mode);
        Set<Long> ids = cats == null || cats.isEmpty() ? Set.of() : cats;
        if (MODE_ALL.equals(m) || ids.isEmpty()) {
            return all;
        }
        BigDecimal matched = sumMatching(lines, ids);
        if (MODE_INCLUDE.equals(m)) {
            return matched;
        }
        return Money.subtract(all, matched);
    }

    /**
     * 品类适用金额。includeCats 空则返回 null（表示用全场剩余）；
     * 非空则汇总命中品类行金额。
     */
    public static BigDecimal eligibleAmount(Collection<AmountLine> lines, Set<Long> includeCats) {
        if (includeCats == null || includeCats.isEmpty()) {
            return null;
        }
        return sumMatching(lines, includeCats);
    }

    /** 券核销基数：有品类限制时取 min(品类合计, 满减后剩余)。 */
    public static BigDecimal voucherBase(BigDecimal afterOffer, BigDecimal eligibleOrNull) {
        BigDecimal remaining = Money.of(afterOffer);
        if (eligibleOrNull == null) {
            return remaining;
        }
        BigDecimal eligible = Money.of(eligibleOrNull);
        return eligible.compareTo(remaining) < 0 ? eligible : remaining;
    }

    public static boolean meetsThreshold(BigDecimal remaining, BigDecimal threshold) {
        return Money.of(remaining).compareTo(Money.of(threshold)) >= 0;
    }

    public static BigDecimal cap(BigDecimal minus, BigDecimal remaining) {
        BigDecimal off = Money.of(minus);
        BigDecimal left = Money.of(remaining);
        if (off.compareTo(Money.zero()) <= 0 || left.compareTo(Money.zero()) <= 0) {
            return Money.zero();
        }
        return off.compareTo(left) > 0 ? left : off;
    }

    private static BigDecimal sumAll(Collection<AmountLine> lines) {
        BigDecimal sum = Money.zero();
        if (lines == null) {
            return sum;
        }
        for (AmountLine line : lines) {
            if (line == null) {
                continue;
            }
            sum = Money.add(sum, line.amount());
        }
        return sum;
    }

    private static BigDecimal sumMatching(Collection<AmountLine> lines, Set<Long> cats) {
        BigDecimal sum = Money.zero();
        if (lines == null || cats == null || cats.isEmpty()) {
            return sum;
        }
        Set<Long> ids = cats instanceof HashSet ? cats : new HashSet<>(cats);
        for (AmountLine line : lines) {
            if (line == null || line.categoryId() == null) {
                continue;
            }
            if (ids.contains(line.categoryId())) {
                sum = Money.add(sum, line.amount());
            }
        }
        return sum;
    }
}
