package com.shishu.open.device.barcode;

import java.math.BigDecimal;

/** 把可选配置（可能为空）补齐为完整版式。 */
public final class ScaleBarcodeRules {

    private ScaleBarcodeRules() {
    }

    public static ScaleBarcodeRule from(
            Integer length,
            String prefix,
            Integer pluDigits,
            String payload,
            BigDecimal factor,
            String checksum
    ) {
        ScaleBarcodeRule def = ScaleBarcodeRule.defaults();
        String pfx = prefix == null || prefix.isBlank() ? def.prefix() : prefix.trim();
        return new ScaleBarcodeRule(
                length == null ? def.length() : length,
                pfx,
                pluDigits == null ? def.pluDigits() : pluDigits,
                payload == null || payload.isBlank() ? def.payload() : payload.trim().toUpperCase(),
                factor == null ? def.factor() : factor,
                checksum == null || checksum.isBlank() ? def.checksum() : checksum.trim().toUpperCase()
        );
    }

    /** 版式自检：返回 null 表示合法，否则返回中文错误原因。 */
    public static String layoutError(ScaleBarcodeRule rule, int pluMax) {
        if (rule.payloadDigits() < 3) {
            return "前缀、PLU 位数与校验位之和须小于条码总长";
        }
        long maxPlu = (long) Math.pow(10, rule.pluDigits()) - 1;
        if (maxPlu < pluMax) {
            return "PLU 位数无法覆盖门店号段上限 " + pluMax;
        }
        if (!ScaleBarcodeRule.WEIGHT.equals(rule.payload()) && !ScaleBarcodeRule.AMOUNT.equals(rule.payload())) {
            return "中间段须为重量码或金额码";
        }
        if (!ScaleBarcodeRule.STRICT.equals(rule.checksum()) && !ScaleBarcodeRule.RELAX.equals(rule.checksum())) {
            return "校验方式无效";
        }
        if (rule.factor() == null || rule.factor().signum() <= 0) {
            return "中间段系数须大于 0";
        }
        return null;
    }
}
