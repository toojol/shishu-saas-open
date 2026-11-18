package com.shishu.open.consign;

import com.shishu.open.consign.ConsignSettlement.SaleLine;
import com.shishu.open.consign.ConsignSettlement.Statement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsignSettlementTest {

    private static final BigDecimal NULL_RATE = null;

    @Test
    void oneDayOnePartnerUsesContractDefault() {
        List<Statement> out = ConsignSettlement.settleByPartner(List.of(
                new SaleLine(1L, new BigDecimal("60.00"), NULL_RATE, new BigDecimal("20")),
                new SaleLine(1L, new BigDecimal("40.00"), NULL_RATE, new BigDecimal("20"))));

        assertEquals(1, out.size());
        Statement s = out.get(0);
        assertEquals(new BigDecimal("100.00"), s.netSales());
        assertEquals(new BigDecimal("20.00"), s.storeTake());
        assertEquals(new BigDecimal("80.00"), s.payableToPartner());
        assertEquals(2, s.lineCount());
    }

    @Test
    void productOverrideBeatsContractDefaultPerLine() {
        // 同一合作方：一行特例 10%，一行跟合同 20%
        List<Statement> out = ConsignSettlement.settleByPartner(List.of(
                new SaleLine(7L, new BigDecimal("100.00"), new BigDecimal("10"), new BigDecimal("20")),
                new SaleLine(7L, new BigDecimal("100.00"), NULL_RATE, new BigDecimal("20"))));

        Statement s = out.get(0);
        assertEquals(new BigDecimal("200.00"), s.netSales());
        assertEquals(new BigDecimal("30.00"), s.storeTake());
        assertEquals(new BigDecimal("170.00"), s.payableToPartner());
        assertEquals(new BigDecimal("15.00"), s.rate());
    }

    @Test
    void multiplePartnersStayApart() {
        List<Statement> out = ConsignSettlement.settleByPartner(List.of(
                new SaleLine(1L, new BigDecimal("50.00"), NULL_RATE, new BigDecimal("10")),
                new SaleLine(2L, new BigDecimal("50.00"), NULL_RATE, new BigDecimal("30"))));

        assertEquals(2, out.size());
        assertEquals(new BigDecimal("5.00"), out.get(0).storeTake());
        assertEquals(new BigDecimal("15.00"), out.get(1).storeTake());
    }

    @Test
    void missingRateFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> ConsignSettlement.settleByPartner(List.of(
                new SaleLine(3L, new BigDecimal("10.00"), NULL_RATE, NULL_RATE))));
    }
}
