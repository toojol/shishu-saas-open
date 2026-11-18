package com.shishu.open.core;

/** 商品类型：标品（STANDARD）与称重品（WEIGH）。 */
public final class ProductTypes {

    public static final String WEIGH = "WEIGH";
    public static final String STANDARD = "STANDARD";

    private ProductTypes() {
    }

    public static boolean isWeigh(String type) {
        return WEIGH.equals(type);
    }

    public static boolean isStandard(String type) {
        return STANDARD.equals(type);
    }
}
