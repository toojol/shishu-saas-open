package com.shishu.open.promo;

import java.util.Set;

/** 满折满赠：买赠 / N 件一口价 / 第 N 件优惠 / 分类与全场折扣 / 满额送。 */
public final class DealKinds {

    public static final String BUY_GET = "BUY_GET";
    public static final String N_FOR_M = "N_FOR_M";
    /** 已迁入 {@link #NTH_FIXED}，保留常量以兼容旧数据。 */
    public static final String SECOND_OFF = "SECOND_OFF";
    public static final String NTH_FIXED = "NTH_FIXED";
    public static final String CATEGORY_OFF = "CATEGORY_OFF";
    public static final String STORE_OFF = "STORE_OFF";
    public static final String THRESHOLD_GIFT = "THRESHOLD_GIFT";

    public static final String NTH_MODE_PERCENT = "PERCENT";
    public static final String NTH_MODE_FIXED = "FIXED";

    private DealKinds() {
    }

    public static Set<String> qtyKinds() {
        return Set.of(BUY_GET, N_FOR_M, SECOND_OFF, NTH_FIXED);
    }

    public static Set<String> scopeKinds() {
        return Set.of(CATEGORY_OFF, STORE_OFF);
    }

    public static Set<String> giftKinds() {
        return Set.of(THRESHOLD_GIFT);
    }

    public static Set<String> all() {
        return Set.of(BUY_GET, N_FOR_M, SECOND_OFF, NTH_FIXED, CATEGORY_OFF, STORE_OFF, THRESHOLD_GIFT);
    }

    public static String normalizeNthMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return NTH_MODE_FIXED;
        }
        String code = mode.trim().toUpperCase();
        return NTH_MODE_PERCENT.equals(code) ? NTH_MODE_PERCENT : NTH_MODE_FIXED;
    }

    public static String normalize(String kind) {
        if (kind == null || kind.isBlank()) {
            return BUY_GET;
        }
        String code = kind.trim().toUpperCase();
        return all().contains(code) ? code : BUY_GET;
    }

    public static boolean isQty(String kind) {
        return qtyKinds().contains(normalize(kind));
    }

    public static boolean isScope(String kind) {
        return scopeKinds().contains(normalize(kind));
    }

    public static boolean isGift(String kind) {
        return giftKinds().contains(normalize(kind));
    }

    public static String label(String kind) {
        return switch (normalize(kind)) {
            case N_FOR_M -> "N件一口价";
            case SECOND_OFF, NTH_FIXED -> "第N件优惠";
            case CATEGORY_OFF -> "分类折扣";
            case STORE_OFF -> "全场折扣";
            case THRESHOLD_GIFT -> "满额送";
            default -> "买赠";
        };
    }
}
