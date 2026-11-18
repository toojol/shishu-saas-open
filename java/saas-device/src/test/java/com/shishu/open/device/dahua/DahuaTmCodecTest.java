package com.shishu.open.device.dahua;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DahuaTmCodecTest {

    @Test
    void toFenRoundsHalfUp() {
        assertEquals(1250, DahuaTmCodec.toFen(new BigDecimal("12.5")));
        assertEquals(126, DahuaTmCodec.toFen(new BigDecimal("1.255")));
    }

    @Test
    void areaCodeForChinese() {
        String area = DahuaTmCodec.toAreaCode("白菜");
        assertTrue(area.length() >= 8);
        assertTrue(area.chars().allMatch(Character::isDigit));
    }

    @Test
    void buildPluFrameStartsWithHeader() {
        byte[] frame = DahuaTmCodec.buildPluFrame(12, "青菜", new BigDecimal("3.50"));
        String ascii = new String(frame, Charset.forName("GB2312"));
        assertTrue(ascii.startsWith("!0V0012A"));
        assertTrue(ascii.contains("000350"));
        assertTrue(ascii.endsWith("CDE\r\n"));
    }

    @Test
    void clearAllPluIsFixedCommand() {
        assertEquals("!0IA\r\n", new String(DahuaTmCodec.clearAllPlu(), Charset.forName("GB2312")));
    }
}
