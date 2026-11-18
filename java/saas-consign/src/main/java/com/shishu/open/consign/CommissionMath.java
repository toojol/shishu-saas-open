package com.shishu.open.consign;

import com.shishu.open.core.Money;

import java.math.BigDecimal;

/** 联营扣点：百分比 0–100，店抽成 = 销售额 × 扣点 / 100。 */
public final class CommissionMath {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private CommissionMath() {
    }

    public static boolean validRate(BigDecimal rate) {
        if (rate == null) {
            return false;
        }
        BigDecimal scaled = scaleRate(rate);
        return scaled.compareTo(BigDecimal.ZERO) >= 0 && scaled.compareTo(HUNDRED) <= 0;
    }

    public static BigDecimal scaleRate(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        return rate.setScale(Money.SCALE, Money.ROUNDING);
    }

    /** 商品特例优先，否则合作方合同默认；都无效返回 null。 */
    public static BigDecimal effectiveRate(BigDecimal productOverride, BigDecimal partnerDefault) {
        if (validRate(productOverride)) {
            return scaleRate(productOverride);
        }
        if (validRate(partnerDefault)) {
            return scaleRate(partnerDefault);
        }
        return null;
    }

    /** 店抽成（销售额 × 扣点%）。 */
    public static BigDecimal storeTake(BigDecimal salesAmount, BigDecimal ratePercent) {
        if (salesAmount == null || ratePercent == null) {
            return Money.zero();
        }
        return Money.of(Money.of(salesAmount).multiply(scaleRate(ratePercent)).divide(HUNDRED, Money.SCALE, Money.ROUNDING));
    }
}
