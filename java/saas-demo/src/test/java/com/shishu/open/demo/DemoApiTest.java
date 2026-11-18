package com.shishu.open.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoApiTest {

    @Test
    void consignSettleSplitsByPartnerAndAppliesProductOverride() {
        String json = DemoApi.consignSettle("""
                1,100,10,20
                1,100,,20
                2,50,,15
                """);
        // 合作方 1：100 按商品特例 10% → 店抽 10；100 按合同 20% → 店抽 20
        assertTrue(json.contains("\"storeTake\":\"30.00\""), json);
        assertTrue(json.contains("\"payableToPartner\":\"170.00\""), json);
        // 合作方 2：50 按合同 15% → 店抽 7.50，应付 42.50
        assertTrue(json.contains("\"storeTake\":\"7.50\""), json);
        assertTrue(json.contains("\"payableToPartner\":\"42.50\""), json);
    }

    @Test
    void consignSettleRejectsBadRowsAndRates() {
        IllegalArgumentException bad = assertThrows(IllegalArgumentException.class,
                () -> DemoApi.consignSettle("1,100"));
        assertTrue(bad.getMessage().contains("第 1 行"), bad.getMessage());

        IllegalArgumentException rate = assertThrows(IllegalArgumentException.class,
                () -> DemoApi.consignSettle("1,100,150,20"));
        assertTrue(rate.getMessage().contains("0～100"), rate.getMessage());

        assertThrows(IllegalArgumentException.class, () -> DemoApi.consignSettle("   \n"));
    }

    @Test
    void dayCloseBalancedWhenTendersMatchSales() {
        String json = DemoApi.dayClose("""
                sales=300
                CASH,120
                WECHAT,100
                ALIPAY,80
                """);
        assertTrue(json.contains("\"balanced\":true"), json);
        assertTrue(json.contains("\"unmatched\":\"0.00\""), json);
        assertTrue(json.contains("\"CASH\":\"120.00\""), json);
    }

    @Test
    void dayCloseShowsUnmatchedAndCashDiff() {
        String json = DemoApi.dayClose("""
                sales=100
                ALIPAY,99.5
                cashExpected=500
                cashCounted=497.5
                """);
        assertFalse(json.contains("\"balanced\":true"), json);
        assertTrue(json.contains("\"unmatched\":\"0.50\""), json);
        assertTrue(json.contains("\"cashDiff\":\"-2.50\""), json);
    }

    @Test
    void dayCloseRejectsUnknownMethodAndMissingSales() {
        IllegalArgumentException method = assertThrows(IllegalArgumentException.class,
                () -> DemoApi.dayClose("sales=10\nBITCOIN,10"));
        assertTrue(method.getMessage().contains("未知支付方式"), method.getMessage());

        IllegalArgumentException sales = assertThrows(IllegalArgumentException.class,
                () -> DemoApi.dayClose("CASH,10"));
        assertTrue(sales.getMessage().contains("sales"), sales.getMessage());
    }

    @Test
    void decodeScaleAcceptsSampleAndExplainsFailures() {
        String ok = DemoApi.decodeScale("2100018012502");
        assertTrue(ok.contains("\"ok\":true"), ok);
        assertTrue(ok.contains("\"plu\":18"), ok);
        assertTrue(ok.contains("\"value\":\"1.250\""), ok);
        assertTrue(ok.contains("\"unit\":\"kg\""), ok);

        String badChecksum = DemoApi.decodeScale("2100018012501");
        assertTrue(badChecksum.contains("\"reason\":\"校验位错误\""), badChecksum);

        String notScale = DemoApi.decodeScale("6901234567892");
        assertTrue(notScale.contains("不是称重码"), notScale);
    }

    @Test
    void pluFrameEncodesDahuaCommand() {
        String json = DemoApi.pluFrame("12", "青菜", "3.50");
        assertTrue(json.contains("!0V0012A"), json);
        assertTrue(json.contains("000350"), json);
        assertTrue(json.contains("CDE"), json);
    }

    @Test
    void pluFrameValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> DemoApi.pluFrame("0", "青菜", "3.5"));
        assertThrows(IllegalArgumentException.class, () -> DemoApi.pluFrame("12", "  ", "3.5"));
        assertThrows(IllegalArgumentException.class, () -> DemoApi.pluFrame("12", "青菜", "-1"));
        assertThrows(IllegalArgumentException.class, () -> DemoApi.pluFrame("abc", "青菜", "1"));
    }
}
