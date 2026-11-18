package com.shishu.open.device.barcode;

import java.math.BigDecimal;

/**
 * 称重码版式：{前缀}{PLU 段}{中间段 payload}{校验位}。
 * 默认 13 位：前缀 "21" + PLU 5 位 + 重量 6 位 + 校验 1 位（每单位 0.001 kg）。
 */
public record ScaleBarcodeRule(
        int length,
        String prefix,
        int pluDigits,
        String payload,
        BigDecimal factor,
        String checksum
) {
    public static final String WEIGHT = "WEIGHT";
    public static final String AMOUNT = "AMOUNT";
    public static final String STRICT = "STRICT";
    public static final String RELAX = "RELAX";

    public static ScaleBarcodeRule defaults() {
        return new ScaleBarcodeRule(13, "21", 5, WEIGHT, new BigDecimal("0.001"), STRICT);
    }

    public int payloadDigits() {
        return length - prefix.length() - pluDigits - 1;
    }

    public boolean weightMode() {
        return WEIGHT.equals(payload);
    }
}
