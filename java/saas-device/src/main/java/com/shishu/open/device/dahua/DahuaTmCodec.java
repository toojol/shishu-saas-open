package com.shishu.open.device.dahua;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;

/**
 * 大华 TM 系列 TCP 文本帧（!0V 下传 PLU）。
 * 品名走 GB2312 区位码；单价单位为分。
 */
public final class DahuaTmCodec {

    private static final Charset GB2312 = Charset.forName("GB2312");
    private static final int NAME_MAX_CHARS = 12;

    private DahuaTmCodec() {
    }

    /** 清全部 PLU：!0IA\r\n */
    public static byte[] clearAllPlu() {
        return "!0IA\r\n".getBytes(GB2312);
    }

    /**
     * 组装一条 PLU 下传帧。
     *
     * @param plu        PLU 号（1～9999）
     * @param name       品名
     * @param retailYuan 零售价（元）
     */
    public static byte[] buildPluFrame(int plu, String name, BigDecimal retailYuan) {
        int safePlu = Math.max(1, Math.min(plu, 9999));
        int fen = toFen(retailYuan);
        String areaName = toAreaCode(trimName(name));
        String code = padLeft(String.valueOf(safePlu), 7, '0');
        StringBuilder sb = new StringBuilder(160);
        sb.append("!0V");
        sb.append(padLeft(String.valueOf(safePlu), 4, '0'));
        sb.append('A');
        sb.append(code);
        sb.append(padLeft(String.valueOf(fen), 6, '0'));
        sb.append('0');                                   // 称重
        sb.append("000000");                              // 特殊信息 ×3
        sb.append("000");                                 // 有效期
        sb.append("01");                                  // 店号
        sb.append("00");                                  // 部门
        sb.append("0000000000000");                       // 数字代号
        sb.append("00000");                               // 皮重（克）
        sb.append("00");                                  // 标签号
        sb.append("00");                                  // 是否打折
        sb.append("000000000000000000000000");            // 四时段折扣
        sb.append('B');
        sb.append(areaName);
        sb.append("CDE\r\n");
        return sb.toString().getBytes(GB2312);
    }

    /** 汉字 / 字符 → 区位码数字串。 */
    public static String toAreaCode(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() * 4);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            byte[] bytes = String.valueOf(ch).getBytes(GB2312);
            for (byte b : bytes) {
                int a = b & 0xFF;
                String t = String.valueOf(a - 0x80 - 0x20);
                if (t.length() == 1) {
                    t = "0" + t;
                }
                out.append(t);
            }
        }
        return out.toString();
    }

    static int toFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0;
        }
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static String trimName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.length() <= NAME_MAX_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, NAME_MAX_CHARS);
    }

    private static String padLeft(String raw, int width, char pad) {
        if (raw.length() >= width) {
            return raw.substring(raw.length() - width);
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = raw.length(); i < width; i++) {
            sb.append(pad);
        }
        sb.append(raw);
        return sb.toString();
    }
}
