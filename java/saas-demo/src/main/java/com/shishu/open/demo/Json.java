package com.shishu.open.demo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 极简 JSON 输出（演示用，不引入第三方依赖）。 */
final class Json {

    private Json() {
    }

    /** 已编码好的 JSON 片段（数组、对象），原样输出。 */
    record Raw(String json) {
    }

    static String object(Map<String, ?> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : fields.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(quote(e.getKey())).append(':').append(value(e.getValue()));
        }
        return sb.append('}').toString();
    }

    static String array(List<String> encodedItems) {
        return "[" + String.join(",", encodedItems) + "]";
    }

    static String value(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Raw raw) {
            return raw.json();
        }
        if (v instanceof BigDecimal bd) {
            // 金额、扣点用字符串输出，避免前端浮点误差
            return quote(bd.toPlainString());
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return quote(v.toString());
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
