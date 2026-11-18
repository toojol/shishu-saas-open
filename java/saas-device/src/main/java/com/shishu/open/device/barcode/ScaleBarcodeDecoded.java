package com.shishu.open.device.barcode;

import java.math.BigDecimal;

/** 解析结果：PLU、中间段原始值、换算值（重量 kg 或金额 元）。 */
public record ScaleBarcodeDecoded(
        int plu,
        int payloadRaw,
        BigDecimal payloadValue,
        boolean checksumOk,
        String payloadType
) {
    public boolean weightMode() {
        return ScaleBarcodeRule.WEIGHT.equals(payloadType);
    }
}
