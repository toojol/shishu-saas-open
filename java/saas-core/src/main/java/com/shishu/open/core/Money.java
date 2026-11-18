package com.shishu.open.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额约定：元为单位，统一 scale=2，四舍五入（HALF_UP）。
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }

    public static BigDecimal of(double value) {
        return of(BigDecimal.valueOf(value));
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return of(of(a).add(of(b)));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return of(of(a).subtract(of(b)));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return of(of(a).multiply(of(b)));
    }

    /** 行金额：单价（2 位）× 数量（可 3 位），结果 2 位。 */
    public static BigDecimal lineAmount(BigDecimal unitPrice, BigDecimal qty) {
        BigDecimal q = qty == null ? BigDecimal.ZERO : qty;
        return of(of(unitPrice).multiply(q));
    }
}
