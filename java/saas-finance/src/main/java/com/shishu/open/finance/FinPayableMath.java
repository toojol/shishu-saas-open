package com.shishu.open.finance;

import com.shishu.open.core.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 应付余额与状态：OPEN / PARTIAL / CLOSED / VOID。 */
public final class FinPayableMath {

    public static final String OPEN = "OPEN";
    public static final String PARTIAL = "PARTIAL";
    public static final String CLOSED = "CLOSED";
    public static final String VOID = "VOID";

    private FinPayableMath() {
    }

    /**
     * 到期日 = 业务日 + 账期天数。账期空视为现结（0 天）。
     * {@code asOf >= dueDate} 即到期 / 超期。
     */
    public static LocalDate dueDate(LocalDate bizDate, Integer settleDays) {
        if (bizDate == null) {
            return null;
        }
        int days = settleDays == null || settleDays < 0 ? 0 : settleDays;
        return bizDate.plusDays(days);
    }

    public static boolean isDue(LocalDate bizDate, Integer settleDays, LocalDate asOf) {
        if (bizDate == null || asOf == null) {
            return false;
        }
        LocalDate due = dueDate(bizDate, settleDays);
        return due != null && !asOf.isBefore(due);
    }

    public static BigDecimal remaining(BigDecimal amount, BigDecimal paidAmount) {
        return Money.subtract(
                amount == null ? Money.zero() : amount,
                paidAmount == null ? Money.zero() : paidAmount);
    }

    /**
     * paid=0 → OPEN；remaining=0 → CLOSED；否则 PARTIAL。
     * 退供负数应付同样适用（paid 向 amount 靠拢）。
     */
    public static String statusOf(BigDecimal amount, BigDecimal paidAmount) {
        BigDecimal paid = paidAmount == null ? Money.zero() : Money.of(paidAmount);
        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            return OPEN;
        }
        if (remaining(amount, paid).compareTo(BigDecimal.ZERO) == 0) {
            return CLOSED;
        }
        return PARTIAL;
    }

    /** 核销额是否在剩余应付范围内（正应付要求 0 &lt; alloc ≤ remaining；负应付要求 remaining ≤ alloc &lt; 0）。 */
    public static boolean allocWithinRemaining(BigDecimal amount, BigDecimal paidAmount, BigDecimal alloc) {
        if (alloc == null) {
            return false;
        }
        BigDecimal rem = remaining(amount, paidAmount);
        BigDecimal a = Money.of(alloc);
        if (rem.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        if (rem.compareTo(BigDecimal.ZERO) > 0) {
            return a.compareTo(BigDecimal.ZERO) > 0 && a.compareTo(rem) <= 0;
        }
        return a.compareTo(BigDecimal.ZERO) < 0 && a.compareTo(rem) >= 0;
    }
}
