package com.shishu.open.consign;

import com.shishu.open.core.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 联营对账：按合作方汇总「分摊后的行净额」，算店抽成与应付合作方。
 * <p>
 * 口径（与 docs/consign.md 一致）：
 * <ul>
 *   <li>扣点 = 商品特例（若有）否则合作方合同默认；</li>
 *   <li>店抽成 = 净销售 × 扣点%；</li>
 *   <li>应付合作方 = 净销售 − 店抽成（即 净销售 × (1 − 扣点%)）。</li>
 * </ul>
 */
public final class ConsignSettlement {

    private ConsignSettlement() {
    }

    /** 单行：已扣除满减、券、整单优惠分摊后的行净额。 */
    public record SaleLine(
            Long partnerId,
            BigDecimal netAmount,
            BigDecimal productOverrideRate,
            BigDecimal partnerDefaultRate) {
    }

    /** 某合作方当日对账结果。 */
    public record Statement(
            Long partnerId,
            BigDecimal netSales,
            BigDecimal rate,
            BigDecimal storeTake,
            BigDecimal payableToPartner,
            int lineCount) {
    }

    /**
     * 按合作方汇总；同一合作方多行按各自生效扣点分别计算店抽成后累加，
     * 避免不同特例扣点被平均化。
     */
    public static List<Statement> settleByPartner(Collection<SaleLine> lines) {
        Map<Long, Acc> acc = new LinkedHashMap<>();
        if (lines == null) {
            return List.of();
        }
        for (SaleLine line : lines) {
            if (line == null || line.partnerId() == null) {
                continue;
            }
            BigDecimal rate = CommissionMath.effectiveRate(line.productOverrideRate(), line.partnerDefaultRate());
            if (rate == null) {
                throw new IllegalArgumentException("合作方 " + line.partnerId() + " 缺少有效扣点");
            }
            BigDecimal net = Money.of(line.netAmount());
            BigDecimal take = CommissionMath.storeTake(net, rate);
            acc.computeIfAbsent(line.partnerId(), k -> new Acc()).add(net, take, rate);
        }
        List<Statement> out = new ArrayList<>();
        for (Map.Entry<Long, Acc> e : acc.entrySet()) {
            out.add(e.getValue().toStatement(e.getKey()));
        }
        return out;
    }

    private static final class Acc {
        private BigDecimal netSales = Money.zero();
        private BigDecimal storeTake = Money.zero();
        private BigDecimal rateWeighted = Money.zero();
        private int lines;

        void add(BigDecimal net, BigDecimal take, BigDecimal rate) {
            netSales = Money.add(netSales, net);
            storeTake = Money.add(storeTake, take);
            rateWeighted = Money.add(rateWeighted, Money.multiply(net, rate));
            lines++;
        }

        Statement toStatement(Long partnerId) {
            BigDecimal blendedRate = netSales.signum() == 0
                    ? null
                    : rateWeighted.divide(netSales, Money.SCALE, Money.ROUNDING);
            BigDecimal payable = Money.subtract(netSales, storeTake);
            return new Statement(partnerId, netSales, blendedRate, storeTake, payable, lines);
        }
    }
}
