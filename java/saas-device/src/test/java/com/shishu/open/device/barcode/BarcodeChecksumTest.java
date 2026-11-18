package com.shishu.open.device.barcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarcodeChecksumTest {

    @Test
    void ean13Samples() {
        assertTrue(BarcodeChecksum.ean13("6901234567892"));
        assertFalse(BarcodeChecksum.ean13("6901234567891"));
    }

    @Test
    void ean8Sample() {
        assertTrue(BarcodeChecksum.ean8("96385074"));
        assertFalse(BarcodeChecksum.ean8("96385075"));
    }

    @Test
    void upcAWithTwelveDigits() {
        assertTrue(BarcodeChecksum.valid("036000291452"));
        assertFalse(BarcodeChecksum.valid("036000291453"));
    }

    @Test
    void lengthsWithoutChecksumRuleAreInvalid() {
        assertFalse(BarcodeChecksum.requiresChecksum("12345"));
        assertFalse(BarcodeChecksum.valid("12345"));
        assertFalse(BarcodeChecksum.allDigits("12a4"));
    }
}
