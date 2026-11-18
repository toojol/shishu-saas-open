package com.shishu.open.device.barcode;

import java.math.BigDecimal;
import java.util.Optional;

/** 电子秤条码解析：前缀匹配 → 校验 → 拆 PLU 与中间段。 */
public final class ScaleBarcodeDecoder {

    private ScaleBarcodeDecoder() {
    }

    public static boolean matchesLayout(String code, ScaleBarcodeRule rule) {
        if (rule == null || !BarcodeChecksum.allDigits(code)) {
            return false;
        }
        if (code.length() != rule.length()) {
            return false;
        }
        if (!code.startsWith(rule.prefix())) {
            return false;
        }
        return rule.payloadDigits() > 0;
    }

    public static Optional<ScaleBarcodeDecoded> decode(String code, ScaleBarcodeRule rule) {
        if (!matchesLayout(code, rule)) {
            return Optional.empty();
        }
        boolean checksumOk = true;
        if (code.length() == 13) {
            checksumOk = BarcodeChecksum.ean13(code);
        } else if (BarcodeChecksum.requiresChecksum(code)) {
            checksumOk = BarcodeChecksum.valid(code);
        }
        if (!checksumOk && ScaleBarcodeRule.STRICT.equals(rule.checksum())) {
            return Optional.empty();
        }
        int prefixLen = rule.prefix().length();
        int pluStart = prefixLen;
        int pluEnd = pluStart + rule.pluDigits();
        int payloadEnd = pluEnd + rule.payloadDigits();
        int plu = Integer.parseInt(code.substring(pluStart, pluEnd));
        int payloadRaw = Integer.parseInt(code.substring(pluEnd, payloadEnd));
        if (plu <= 0) {
            return Optional.empty();
        }
        BigDecimal value = rule.factor().multiply(BigDecimal.valueOf(payloadRaw));
        return Optional.of(new ScaleBarcodeDecoded(plu, payloadRaw, value, checksumOk, rule.payload()));
    }
}
