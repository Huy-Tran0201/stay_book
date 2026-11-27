package com.staybook.staybook.common.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormat {
    private static final DecimalFormatSymbols SYMBOLS;

    static {
        SYMBOLS = new DecimalFormatSymbols(Locale.GERMAN);
        SYMBOLS.setGroupingSeparator('.');
    }

    private MoneyFormat() {}

    public static String format(long amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0", SYMBOLS);
        return formatter.format(amount);
    }

    public static String formatWithUnit(long amount) {
        return format(amount) + " ₫";
    }
}
