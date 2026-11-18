package com.shishu.open.core;

/** 经营方式：购销（PURCHASE）与联营（CONSIGN）。 */
public final class BizModes {

    public static final String PURCHASE = "PURCHASE";
    public static final String CONSIGN = "CONSIGN";

    private BizModes() {
    }

    public static boolean isConsign(String mode) {
        return CONSIGN.equals(mode);
    }

    public static boolean isPurchase(String mode) {
        return !isConsign(mode);
    }
}
