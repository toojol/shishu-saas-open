package com.shishu.open.device.barcode;

/** EAN-8 / UPC-A / EAN-13 校验位。 */
public final class BarcodeChecksum {

    private BarcodeChecksum() {
    }

    public static boolean allDigits(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            if (!Character.isDigit(code.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean requiresChecksum(String code) {
        if (!allDigits(code)) {
            return false;
        }
        int n = code.length();
        return n == 8 || n == 12 || n == 13;
    }

    public static boolean valid(String code) {
        if (!requiresChecksum(code)) {
            return false;
        }
        if (code.length() == 12) {
            return ean13("0" + code);
        }
        if (code.length() == 8) {
            return ean8(code);
        }
        return ean13(code);
    }

    /** EAN-13：从左 0 起偶数位×1、奇数位×3。 */
    public static boolean ean13(String code) {
        if (code == null || code.length() != 13 || !allDigits(code)) {
            return false;
        }
        return checkDigit(code.substring(0, 12), true) == code.charAt(12) - '0';
    }

    /** EAN-8：从左 0 起偶数位×3、奇数位×1。 */
    public static boolean ean8(String code) {
        if (code == null || code.length() != 8 || !allDigits(code)) {
            return false;
        }
        return checkDigit(code.substring(0, 7), false) == code.charAt(7) - '0';
    }

    public static int checkDigit(String body, boolean ean13Style) {
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            int d = body.charAt(i) - '0';
            if (ean13Style) {
                sum += (i % 2 == 0) ? d : d * 3;
            } else {
                sum += (i % 2 == 0) ? d * 3 : d;
            }
        }
        return (10 - (sum % 10)) % 10;
    }
}
