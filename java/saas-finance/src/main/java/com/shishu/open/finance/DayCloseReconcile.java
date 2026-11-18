package com.shishu.open.finance;

import com.shishu.open.core.Money;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * 日结对账：把一天的支付拆分汇总，并与应收销售额比对；
 * 现金另算长短款（实点现金 − 应有现金）。
 * <p>
 * 纯函数，不依赖数据库。日结单的持久化、封账与导出不在本仓范围内。
 */
public final class DayCloseReconcile {

    private DayCloseReconcile() {
    }

    /** 支付方式。 */
    public enum Method {
        CASH,
        WECHAT,
        ALIPAY,
        STORED_VALUE,
        OTHER
    }

    /** 一笔支付拆分。金额为正，退款用负数。 */
    public record Tender(Method method, BigDecimal amount) {
    }

    /** 日结汇总。 */
    public record Summary(
            Map<Method, BigDecimal> byMethod,
            BigDecimal tenderTotal,
            BigDecimal salesTotal,
            BigDecimal unmatched) {

        /** 有差额即需要人工查（销售额与收款合计对不上）。 */
        public boolean balanced() {
            return unmatched.signum() == 0;
        }
    }

    public static Summary summarize(BigDecimal salesTotal, Collection<Tender> tenders) {
        Map<Method, BigDecimal> by = new EnumMap<>(Method.class);
        for (Method m : Method.values()) {
            by.put(m, Money.zero());
        }
        BigDecimal total = Money.zero();
        if (tenders != null) {
            for (Tender t : tenders) {
                if (t == null || t.method() == null) {
                    continue;
                }
                BigDecimal amt = Money.of(t.amount());
                by.put(t.method(), Money.add(by.get(t.method()), amt));
                total = Money.add(total, amt);
            }
        }
        BigDecimal sales = Money.of(salesTotal);
        return new Summary(by, total, sales, Money.subtract(sales, total));
    }

    /**
     * 现金长短款：实点现金 − 应有现金（应有 = 现金收款 − 现金找零 − 备用金以外的现金支出，由调用方算好传入）。
     * 正数为长款，负数为短款。
     */
    public static BigDecimal cashDiff(BigDecimal expectedCash, BigDecimal countedCash) {
        return Money.subtract(countedCash, expectedCash);
    }
}
