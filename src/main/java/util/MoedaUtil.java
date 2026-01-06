package util;

import java.text.NumberFormat;
import java.util.Locale;

public class MoedaUtil {
    private static final Locale LOCALE_BR = new Locale("pt", "BR");
    private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(LOCALE_BR);
    private static final NumberFormat NUM = NumberFormat.getNumberInstance(LOCALE_BR);
    private static final NumberFormat PCT = NumberFormat.getPercentInstance(LOCALE_BR);

    static {
        PCT.setMinimumFractionDigits(0);
        PCT.setMaximumFractionDigits(1);
    }

    public static String brl(double v) { return BRL.format(v); }
    public static String numero(double v) { return NUM.format(v); }
    public static String pct(double v) { return PCT.format(v); } // v=0.12 => "12%"
}
