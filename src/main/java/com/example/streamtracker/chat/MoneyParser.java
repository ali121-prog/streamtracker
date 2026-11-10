package com.example.streamtracker.chat;

import java.util.Locale;

public final class MoneyParser {
    private MoneyParser() {}

    /** Parses "$1,500", "10M", "1.5B", "2T". Returns -1 on failure. */
    public static double parse(String s) {
        if (s == null) return -1;
        s = s.trim().replace("$", "").replace(",", "").replace("_", "");
        if (s.isEmpty()) return -1;

        double mult = 1;
        char last = Character.toUpperCase(s.charAt(s.length() - 1));
        if (Character.isLetter(last)) {
            switch (last) {
                case 'K' -> mult = 1e3;
                case 'M' -> mult = 1e6;
                case 'B' -> mult = 1e9;
                case 'T' -> mult = 1e12;
                default -> { return -1; }
            }
            s = s.substring(0, s.length() - 1);
        }
        try {
            return Double.parseDouble(s) * mult;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 1_500_000_000 -> "1.5B" */
    public static String format(double v) {
        if (v >= 1e12) return trim(v / 1e12) + "T";
        if (v >= 1e9) return trim(v / 1e9) + "B";
        if (v >= 1e6) return trim(v / 1e6) + "M";
        if (v >= 1e3) return trim(v / 1e3) + "K";
        return trim(v);
    }

    private static String trim(double x) {
        String t = String.format(Locale.ROOT, "%.2f", x);
        if (t.contains(".")) t = t.replaceAll("0+$", "").replaceAll("\\.$", "");
        return t;
    }
}
