package com.shishu.open.device.barcode;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScaleBarcodeDecoderTest {

    private final ScaleBarcodeRule rule = ScaleBarcodeRule.defaults();

    @Test
    void ean13SampleDecodesPluAndKg() {
        String code = "2100018012502";
        assertTrue(BarcodeChecksum.ean13(code));
        Optional<ScaleBarcodeDecoded> decoded = ScaleBarcodeDecoder.decode(code, rule);
        assertTrue(decoded.isPresent());
        assertEquals(18, decoded.get().plu());
        assertEquals(0, new BigDecimal("1.250").compareTo(decoded.get().payloadValue()));
        assertTrue(decoded.get().weightMode());
    }

    @Test
    void strictRejectsBadChecksum() {
        assertTrue(ScaleBarcodeDecoder.decode("2100018012501", rule).isEmpty());
    }

    @Test
    void relaxAcceptsBadChecksum() {
        ScaleBarcodeRule relax = new ScaleBarcodeRule(13, "21", 5, ScaleBarcodeRule.WEIGHT,
                new BigDecimal("0.001"), ScaleBarcodeRule.RELAX);
        Optional<ScaleBarcodeDecoded> decoded = ScaleBarcodeDecoder.decode("2100018012501", relax);
        assertTrue(decoded.isPresent());
        assertEquals(18, decoded.get().plu());
        assertFalse(decoded.get().checksumOk());
    }

    @Test
    void otherPrefixNotScale() {
        assertFalse(ScaleBarcodeDecoder.matchesLayout("6901234567892", rule));
    }

    @Test
    void pluZeroIsRejected() {
        // PLU 段全 0 视为无效；校验位需为合法值，这里用 RELAX 绕开校验，只测 PLU 判断
        ScaleBarcodeRule relax = new ScaleBarcodeRule(13, "21", 5, ScaleBarcodeRule.WEIGHT,
                new BigDecimal("0.001"), ScaleBarcodeRule.RELAX);
        assertTrue(ScaleBarcodeDecoder.decode("2100000012500", relax).isEmpty());
    }
}
