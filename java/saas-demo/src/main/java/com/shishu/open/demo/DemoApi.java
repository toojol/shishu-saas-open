package com.shishu.open.demo;

import com.shishu.open.consign.CommissionMath;
import com.shishu.open.consign.ConsignSettlement;
import com.shishu.open.consign.ConsignSettlement.SaleLine;
import com.shishu.open.consign.ConsignSettlement.Statement;
import com.shishu.open.device.barcode.BarcodeChecksum;
import com.shishu.open.device.barcode.ScaleBarcodeDecoded;
import com.shishu.open.device.barcode.ScaleBarcodeDecoder;
import com.shishu.open.device.barcode.ScaleBarcodeRule;
import com.shishu.open.device.dahua.DahuaTmCodec;
import com.shishu.open.finance.DayCloseReconcile;
import com.shishu.open.finance.DayCloseReconcile.Method;
import com.shishu.open.finance.DayCloseReconcile.Summary;
import com.shishu.open.finance.DayCloseReconcile.Tender;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 演示接口的业务逻辑：输入文本，调用开源规则代码，输出 JSON。
 * 错误输入统一抛 {@link IllegalArgumentException}，由服务层转成 400。
 */
public final class DemoApi {

    private static final Charset GB2312 = Charset.forName("GB2312");

    private DemoApi() {
    }

    /**
     * 联营对账。每行：合作方ID,行净额,商品扣点(可空),合同默认扣点
     * 扣点单位为百分比，例如 20 表示 20%。
     */
    public static String consignSettle(String body) {
        List<SaleLine> lines = new ArrayList<>();
        String[] rows = body == null ? new String[0] : body.split("\\r?\\n");
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i].trim();
            if (row.isEmpty() || row.startsWith("#")) {
                continue;
            }
            String[] p = row.split(",", -1);
            if (p.length != 4) {
                throw new IllegalArgumentException(
                        "第 " + (i + 1) + " 行格式应为：合作方ID,行净额,商品扣点(可空),合同扣点");
            }
            Long partnerId = parseLong(p[0], "合作方ID", i + 1);
            BigDecimal net = parseMoney(p[1], "行净额", i + 1);
            BigDecimal productRate = parseOptionalRate(p[2], "商品扣点", i + 1);
            BigDecimal partnerRate = parseOptionalRate(p[3], "合同扣点", i + 1);
            lines.add(new SaleLine(partnerId, net, productRate, partnerRate));
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("请至少输入一行销售明细");
        }
        List<Statement> statements = ConsignSettlement.settleByPartner(lines);
        List<String> items = new ArrayList<>();
        for (Statement s : statements) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("partnerId", s.partnerId());
            m.put("lineCount", s.lineCount());
            m.put("netSales", s.netSales());
            m.put("rate", s.rate());
            m.put("storeTake", s.storeTake());
            m.put("payableToPartner", s.payableToPartner());
            items.add(Json.object(m));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("statements", new Json.Raw(Json.array(items)));
        return Json.object(out);
    }

    /**
     * 日结对账。第一条非空行：sales=应收销售额；随后每行：支付方式,金额（退款用负数）。
     * 可选：cashExpected=应有现金、cashCounted=实点现金，用于现金长短款。
     */
    public static String dayClose(String body) {
        BigDecimal sales = null;
        BigDecimal cashExpected = null;
        BigDecimal cashCounted = null;
        List<Tender> tenders = new ArrayList<>();
        String[] rows = body == null ? new String[0] : body.split("\\r?\\n");
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i].trim();
            if (row.isEmpty() || row.startsWith("#")) {
                continue;
            }
            int eq = row.indexOf('=');
            if (eq > 0 && row.indexOf(',') < 0) {
                String key = row.substring(0, eq).trim();
                String val = row.substring(eq + 1);
                switch (key) {
                    case "sales" -> sales = parseMoney(val, "sales", i + 1);
                    case "cashExpected" -> cashExpected = parseMoney(val, "cashExpected", i + 1);
                    case "cashCounted" -> cashCounted = parseMoney(val, "cashCounted", i + 1);
                    default -> throw new IllegalArgumentException("第 " + (i + 1) + " 行未知字段：" + key);
                }
                continue;
            }
            int comma = row.indexOf(',');
            if (comma < 0) {
                throw new IllegalArgumentException("第 " + (i + 1) + " 行格式应为：支付方式,金额");
            }
            Method method = parseMethod(row.substring(0, comma), i + 1);
            BigDecimal amount = parseMoney(row.substring(comma + 1), "金额", i + 1);
            tenders.add(new Tender(method, amount));
        }
        if (sales == null) {
            throw new IllegalArgumentException("缺少 sales=应收销售额");
        }
        Summary s = DayCloseReconcile.summarize(sales, tenders);

        Map<String, Object> byMethod = new LinkedHashMap<>();
        EnumMap<Method, BigDecimal> sorted = new EnumMap<>(Method.class);
        sorted.putAll(s.byMethod());
        for (Map.Entry<Method, BigDecimal> e : sorted.entrySet()) {
            byMethod.put(e.getKey().name(), e.getValue());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("salesTotal", s.salesTotal());
        out.put("tenderTotal", s.tenderTotal());
        out.put("unmatched", s.unmatched());
        out.put("balanced", s.balanced());
        out.put("byMethod", new Json.Raw(Json.object(byMethod)));
        if (cashExpected != null && cashCounted != null) {
            out.put("cashDiff", DayCloseReconcile.cashDiff(cashExpected, cashCounted));
        }
        return Json.object(out);
    }

    /** 电子秤条码（默认 13 位 · 前缀 21 · 5 位 PLU · 6 位重量 · 校验位）。 */
    public static String decodeScale(String code) {
        String c = code == null ? "" : code.trim();
        ScaleBarcodeRule rule = ScaleBarcodeRule.defaults();
        Optional<ScaleBarcodeDecoded> decoded = ScaleBarcodeDecoder.decode(c, rule);
        Map<String, Object> out = new LinkedHashMap<>();
        if (decoded.isPresent()) {
            ScaleBarcodeDecoded d = decoded.get();
            out.put("ok", true);
            out.put("plu", d.plu());
            out.put("payloadRaw", d.payloadRaw());
            out.put("value", d.payloadValue());
            out.put("unit", d.weightMode() ? "kg" : "元");
            out.put("checksumOk", d.checksumOk());
            return Json.object(out);
        }
        String reason;
        if (!ScaleBarcodeDecoder.matchesLayout(c, rule)) {
            reason = "不是称重码：应为 13 位数字且以 21 开头";
        } else if (c.length() == 13 && !BarcodeChecksum.ean13(c)) {
            reason = "校验位错误";
        } else {
            reason = "PLU 为 0，无效";
        }
        out.put("ok", false);
        out.put("reason", reason);
        return Json.object(out);
    }

    /** PLU 下发帧（!0V…），价格单位元，品名按 GB2312 区位码编码。 */
    public static String pluFrame(String pluText, String name, String priceText) {
        int plu = parseInt(pluText, "PLU 号");
        if (plu < 1 || plu > 9999) {
            throw new IllegalArgumentException("PLU 号范围为 1～9999");
        }
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("品名不能为空");
        }
        BigDecimal price = parseMoney(priceText, "零售价", 0);
        if (price.signum() < 0) {
            throw new IllegalArgumentException("零售价不能为负数");
        }
        byte[] frame = DahuaTmCodec.buildPluFrame(plu, trimmed, price);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("frame", new String(frame, GB2312));
        out.put("bytes", frame.length);
        out.put("nameAreaCode", DahuaTmCodec.toAreaCode(trimmed.length() > 12 ? trimmed.substring(0, 12) : trimmed));
        return Json.object(out);
    }

    // ---- 解析工具 ----

    private static BigDecimal parseOptionalRate(String raw, String label, int line) {
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        BigDecimal rate = parseMoney(t, label, line);
        if (!CommissionMath.validRate(rate)) {
            throw new IllegalArgumentException("第 " + line + " 行" + label + "须在 0～100 之间");
        }
        return rate;
    }

    private static BigDecimal parseMoney(String raw, String label, int line) {
        if (raw == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            String where = line > 0 ? "第 " + line + " 行" : "";
            throw new IllegalArgumentException(where + label + "不是有效数字：" + raw.trim());
        }
    }

    private static Long parseLong(String raw, String label, int line) {
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("第 " + line + " 行" + label + "须为整数");
        }
    }

    private static int parseInt(String raw, String label) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "须为整数");
        }
    }

    private static Method parseMethod(String raw, int line) {
        String key = raw.trim().toUpperCase();
        try {
            return Method.valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("第 " + line + " 行未知支付方式：" + raw.trim()
                    + "（可选 CASH, WECHAT, ALIPAY, STORED_VALUE, OTHER）");
        }
    }
}
