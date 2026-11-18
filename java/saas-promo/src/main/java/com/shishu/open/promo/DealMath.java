package com.shishu.open.promo;

import com.shishu.open.core.BizModes;
import com.shishu.open.core.Money;
import com.shishu.open.core.ProductTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 件折 / 买赠 整单减免；分类与全场折扣行价百分比；满额送。
 * 同商品多件折取 priority 更高；不与另一件折叠在同一商品上。
 */
public final class DealMath {

    private static final BigDecimal MIN = new BigDecimal("0.01");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private DealMath() {
    }

    public record Line(
            Long productId,
            Long categoryId,
            String productType,
            String bizMode,
            String priceKind,
            BigDecimal qty,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }

    public record Rule(
            Long id,
            String name,
            String kind,
            BigDecimal buyQty,
            BigDecimal getQty,
            BigDecimal nQty,
            BigDecimal nAmount,
            BigDecimal secondPercent,
            BigDecimal nthQty,
            BigDecimal nthAmount,
            String nthMode,
            BigDecimal discountPercent,
            boolean excludeWeigh,
            boolean excludeConsign,
            int priority,
            boolean memberOnly,
            Set<Long> productIds,
            Set<Long> categoryIds,
            BigDecimal thresholdAmount,
            Long giftProductId,
            String giftProductName,
            BigDecimal giftQty,
            BigDecimal perOrderQty,
            BigDecimal perMemberQty
    ) {
    }

    public record Pick(Long id, String name, BigDecimal amount) {
        public static Pick none() {
            return new Pick(null, null, Money.zero());
        }

        public boolean present() {
            return id != null && amount != null && amount.compareTo(Money.zero()) > 0;
        }
    }

    public record GiftPick(
            Long id,
            String name,
            Long giftProductId,
            String giftProductName,
            BigDecimal giftQty
    ) {
        public static GiftPick none() {
            return new GiftPick(null, null, null, null, null);
        }

        public boolean present() {
            return id != null && giftProductId != null && giftQty != null
                    && giftQty.compareTo(MIN) > 0;
        }
    }

    /** 件折 / 买赠：在满减之后取减免最大一档（同额取 priority 更高）。 */
    public static Pick bestQtyDeal(List<Rule> rules, List<Line> lines, boolean hasMember) {
        if (rules == null || lines == null || lines.isEmpty()) {
            return Pick.none();
        }
        Pick best = Pick.none();
        BigDecimal bestAmt = Money.zero();
        int bestPriority = Integer.MIN_VALUE;
        for (Rule rule : rules) {
            if (rule == null || rule.id() == null || !DealKinds.isQty(rule.kind())) {
                continue;
            }
            if (rule.memberOnly() && !hasMember) {
                continue;
            }
            BigDecimal amt = qtyDiscount(rule, lines);
            if (amt.compareTo(MIN) < 0) {
                continue;
            }
            if (amt.compareTo(bestAmt) > 0
                    || (amt.compareTo(bestAmt) == 0 && rule.priority() > bestPriority)) {
                bestAmt = amt;
                bestPriority = rule.priority();
                best = new Pick(rule.id(), rule.name(), amt);
            }
        }
        return best;
    }

    /** 满额送：取已满足门槛中门槛最高的一档（同门槛取 priority 更高）。 */
    public static GiftPick pickGift(List<Rule> rules, BigDecimal lineSum, boolean hasMember) {
        if (rules == null || lineSum == null || lineSum.compareTo(MIN) < 0) {
            return GiftPick.none();
        }
        GiftPick best = GiftPick.none();
        BigDecimal bestThreshold = null;
        int bestPriority = Integer.MIN_VALUE;
        for (Rule rule : rules) {
            if (rule == null || rule.id() == null || !DealKinds.isGift(rule.kind())) {
                continue;
            }
            if (rule.memberOnly() && !hasMember) {
                continue;
            }
            BigDecimal threshold = Money.of(rule.thresholdAmount());
            if (threshold.compareTo(MIN) < 0 || lineSum.compareTo(threshold) < 0) {
                continue;
            }
            if (rule.giftProductId() == null || rule.giftQty() == null
                    || rule.giftQty().compareTo(MIN) <= 0) {
                continue;
            }
            if (bestThreshold == null
                    || threshold.compareTo(bestThreshold) > 0
                    || (threshold.compareTo(bestThreshold) == 0 && rule.priority() > bestPriority)) {
                bestThreshold = threshold;
                bestPriority = rule.priority();
                best = new GiftPick(
                        rule.id(),
                        rule.name(),
                        rule.giftProductId(),
                        rule.giftProductName(),
                        rule.giftQty().setScale(3, RoundingMode.HALF_UP));
            }
        }
        return best;
    }

    /**
     * 分类 / 全场折扣：相对零售的应付单价百分比（如 90 = 九折）。
     * 多档命中同一商品取 priority 更高；再与特价等行价取低由上层完成。
     */
    public static BigDecimal scopePercentForProduct(
            List<Rule> rules,
            Long productId,
            Long categoryId,
            String productType,
            String bizMode,
            boolean hasMember) {
        if (rules == null || productId == null) {
            return null;
        }
        BigDecimal bestPercent = null;
        int bestPriority = Integer.MIN_VALUE;
        for (Rule rule : rules) {
            if (rule == null || !DealKinds.isScope(rule.kind())) {
                continue;
            }
            if (rule.memberOnly() && !hasMember) {
                continue;
            }
            if (!scopeMatches(rule, productId, categoryId, productType, bizMode)) {
                continue;
            }
            BigDecimal pct = rule.discountPercent();
            if (pct == null || pct.compareTo(MIN) < 0 || pct.compareTo(HUNDRED) > 0) {
                continue;
            }
            if (bestPercent == null || rule.priority() > bestPriority
                    || (rule.priority() == bestPriority && pct.compareTo(bestPercent) < 0)) {
                bestPercent = Money.of(pct);
                bestPriority = rule.priority();
            }
        }
        return bestPercent;
    }

    public static BigDecimal scopeUnitPrice(BigDecimal retailPrice, BigDecimal percent) {
        if (percent == null) {
            return null;
        }
        BigDecimal retail = Money.of(retailPrice);
        BigDecimal pct = Money.of(percent);
        if (retail.compareTo(MIN) < 0 || pct.compareTo(MIN) < 0) {
            return null;
        }
        BigDecimal price = retail.multiply(pct).divide(HUNDRED, Money.SCALE, RoundingMode.HALF_UP);
        return price.compareTo(MIN) < 0 ? MIN : price;
    }

    static BigDecimal qtyDiscount(Rule rule, List<Line> lines) {
        String kind = DealKinds.normalize(rule.kind());
        BigDecimal capFactor = capFactor(rule, lines);
        BigDecimal total;
        if (DealKinds.NTH_FIXED.equals(kind)) {
            total = nthOff(rule, lines);
        } else {
            total = Money.zero();
            for (LineAgg agg : aggregateEligible(rule, lines)) {
                BigDecimal off = switch (kind) {
                    case DealKinds.BUY_GET -> buyGetOff(agg, rule.buyQty(), rule.getQty());
                    case DealKinds.N_FOR_M -> nForMOff(agg, rule.nQty(), rule.nAmount());
                    case DealKinds.SECOND_OFF -> secondOff(agg, rule.secondPercent());
                    default -> Money.zero();
                };
                total = Money.add(total, off);
            }
        }
        if (capFactor.compareTo(BigDecimal.ONE) < 0) {
            total = Money.of(total.multiply(capFactor));
        }
        return total;
    }

    private static BigDecimal capFactor(Rule rule, List<Line> lines) {
        if (rule.perOrderQty() == null) {
            return BigDecimal.ONE;
        }
        BigDecimal total = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        for (LineAgg agg : aggregateEligible(rule, lines)) {
            total = total.add(agg.qty);
        }
        if (total.compareTo(MIN) < 0 || total.compareTo(rule.perOrderQty()) <= 0) {
            return BigDecimal.ONE;
        }
        return rule.perOrderQty().divide(total, 6, RoundingMode.HALF_UP);
    }

    private static boolean scopeMatches(Rule rule, Long productId, Long categoryId, String productType, String bizMode) {
        if (rule.excludeWeigh() && ProductTypes.isWeigh(productType)) {
            return false;
        }
        if (rule.excludeConsign() && BizModes.isConsign(bizMode)) {
            return false;
        }
        if (DealKinds.STORE_OFF.equals(DealKinds.normalize(rule.kind()))) {
            return true;
        }
        return rule.categoryIds() != null && categoryId != null && rule.categoryIds().contains(categoryId);
    }

    private static boolean qtyProductEligible(Rule rule, Line line) {
        if (line.productId() == null || line.qty() == null || line.qty().compareTo(MIN) < 0) {
            return false;
        }
        if (ProductTypes.isWeigh(line.productType())) {
            return false;
        }
        return rule.productIds() != null && rule.productIds().contains(line.productId());
    }

    private static List<LineAgg> aggregateEligible(Rule rule, List<Line> lines) {
        List<LineAgg> out = new ArrayList<>();
        for (Line line : lines) {
            if (!qtyProductEligible(rule, line)) {
                continue;
            }
            LineAgg found = null;
            for (LineAgg agg : out) {
                if (agg.productId.equals(line.productId())
                        && agg.unitPrice.compareTo(Money.of(line.unitPrice())) == 0) {
                    found = agg;
                    break;
                }
            }
            if (found == null) {
                found = new LineAgg(line.productId(), Money.of(line.unitPrice()), Money.zero());
                out.add(found);
            }
            found.qty = found.qty.add(line.qty().setScale(3, RoundingMode.HALF_UP));
        }
        out.sort(Comparator.comparing((LineAgg a) -> a.unitPrice).thenComparing(a -> a.productId));
        return out;
    }

    /** 买 X 赠 Y：每 (X+Y) 件免 Y 件（按单价，同品同价合并）。 */
    static BigDecimal buyGetOff(LineAgg agg, BigDecimal buyQty, BigDecimal getQty) {
        BigDecimal buy = qtyParam(buyQty);
        BigDecimal get = qtyParam(getQty);
        if (buy.compareTo(MIN) < 0 || get.compareTo(MIN) < 0) {
            return Money.zero();
        }
        BigDecimal group = buy.add(get);
        BigDecimal groups = agg.qty.divide(group, 0, RoundingMode.DOWN);
        BigDecimal freeQty = groups.multiply(get).setScale(3, RoundingMode.HALF_UP);
        return Money.lineAmount(agg.unitPrice, freeQty);
    }

    /** N 件 M 元。 */
    static BigDecimal nForMOff(LineAgg agg, BigDecimal nQty, BigDecimal nAmount) {
        BigDecimal n = qtyParam(nQty);
        BigDecimal pay = Money.of(nAmount);
        if (n.compareTo(MIN) < 0 || pay.compareTo(Money.zero()) < 0) {
            return Money.zero();
        }
        BigDecimal groups = agg.qty.divide(n, 0, RoundingMode.DOWN);
        if (groups.compareTo(BigDecimal.ZERO) <= 0) {
            return Money.zero();
        }
        BigDecimal normal = Money.lineAmount(agg.unitPrice, groups.multiply(n));
        BigDecimal special = Money.of(pay.multiply(groups));
        BigDecimal off = Money.subtract(normal, special);
        return off.compareTo(Money.zero()) > 0 ? off : Money.zero();
    }

    /** 第 2 件折：每 2 件中第 2 件按 percent 折（50 = 五折）。 */
    static BigDecimal secondOff(LineAgg agg, BigDecimal secondPercent) {
        BigDecimal pct = Money.of(secondPercent);
        if (pct.compareTo(MIN) < 0 || pct.compareTo(HUNDRED) > 0) {
            return Money.zero();
        }
        BigDecimal pairs = agg.qty.divide(new BigDecimal("2"), 0, RoundingMode.DOWN);
        if (pairs.compareTo(BigDecimal.ZERO) <= 0) {
            return Money.zero();
        }
        BigDecimal payRate = pct.divide(HUNDRED, 4, RoundingMode.HALF_UP);
        BigDecimal offRate = BigDecimal.ONE.subtract(payRate);
        BigDecimal offUnit = Money.of(agg.unitPrice.multiply(offRate));
        return Money.lineAmount(offUnit, pairs);
    }

    /**
     * 第 N 件优惠：活动商品池跨 SKU 展开，单价降序；
     * FIXED 模式第 N 件按固定额；PERCENT 模式第 N 件按折扣百分比。
     */
    static BigDecimal nthOff(Rule rule, List<Line> lines) {
        BigDecimal n = qtyParam(rule.nthQty());
        if (n.compareTo(new BigDecimal("2")) < 0) {
            return Money.zero();
        }
        int step = n.setScale(0, RoundingMode.DOWN).intValue();
        if (step < 2) {
            return Money.zero();
        }
        boolean percentMode = DealKinds.NTH_MODE_PERCENT.equals(DealKinds.normalizeNthMode(rule.nthMode()));
        BigDecimal fixed = Money.of(rule.nthAmount());
        BigDecimal pct = Money.of(rule.secondPercent());
        if (percentMode) {
            if (pct.compareTo(MIN) < 0 || pct.compareTo(HUNDRED) > 0) {
                return Money.zero();
            }
        } else if (fixed.compareTo(Money.zero()) < 0) {
            return Money.zero();
        }
        List<BigDecimal> units = expandEligibleUnits(rule, lines);
        if (units.size() < step) {
            return Money.zero();
        }
        units.sort(Comparator.reverseOrder());
        BigDecimal off = Money.zero();
        BigDecimal payRate = percentMode
                ? pct.divide(HUNDRED, 4, RoundingMode.HALF_UP)
                : null;
        for (int i = 0; i < units.size(); i++) {
            if ((i + 1) % step != 0) {
                continue;
            }
            BigDecimal unit = units.get(i);
            if (percentMode) {
                BigDecimal offUnit = Money.of(unit.multiply(BigDecimal.ONE.subtract(payRate)));
                off = Money.add(off, offUnit);
            } else {
                BigDecimal pay = unit.compareTo(fixed) < 0 ? unit : fixed;
                off = Money.add(off, Money.subtract(unit, pay));
            }
        }
        return off;
    }

    private static List<BigDecimal> expandEligibleUnits(Rule rule, List<Line> lines) {
        List<BigDecimal> units = new ArrayList<>();
        if (lines == null) {
            return units;
        }
        for (Line line : lines) {
            if (!qtyProductEligible(rule, line)) {
                continue;
            }
            int pieces = line.qty().setScale(0, RoundingMode.DOWN).intValue();
            if (pieces <= 0) {
                continue;
            }
            BigDecimal unit = Money.of(line.unitPrice());
            for (int i = 0; i < pieces; i++) {
                units.add(unit);
            }
        }
        return units;
    }

    private static BigDecimal qtyParam(BigDecimal raw) {
        if (raw == null) {
            return Money.zero().setScale(3, RoundingMode.HALF_UP);
        }
        return raw.setScale(3, RoundingMode.HALF_UP);
    }

    static final class LineAgg {
        final Long productId;
        final BigDecimal unitPrice;
        BigDecimal qty;

        LineAgg(Long productId, BigDecimal unitPrice, BigDecimal qty) {
            this.productId = productId;
            this.unitPrice = unitPrice;
            this.qty = qty;
        }
    }
}
