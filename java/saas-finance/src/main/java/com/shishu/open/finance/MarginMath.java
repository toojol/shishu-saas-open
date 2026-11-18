package com.shishu.open.finance;

import com.shishu.open.core.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 毛利与毛利率。 */
public final class MarginMath {

    private MarginMath() {
    }

    /** 毛利率百分比（2 位，HALF_UP）；净销售为 0 或 null 时返回 null。 */
    public static BigDecimal rate(BigDecimal profit, BigDecimal netSales) {
        if (netSales == null || netSales.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal p = profit == null ? Money.zero() : profit;
        return p.multiply(new BigDecimal("100")).divide(netSales, 2, RoundingMode.HALF_UP);
    }
}
