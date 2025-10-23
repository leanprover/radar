package org.leanlang.radar;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

public record Formatter(boolean sign, int precision) {
    public Formatter() {
        this(false, 1);
    }

    public Formatter withSign(boolean sign) {
        return new Formatter(sign, precision);
    }

    public Formatter withPrecision(int precision) {
        return new Formatter(sign, precision);
    }

    private String formatFloat(float value) {
        if (precision == 0) return Math.round(value) + "";
        if (precision > 0) return String.format(Locale.ENGLISH, "%." + precision + "f", value);
        throw new IllegalArgumentException("precision must not be negative");
    }

    private record Prefix(double value, String symbol) {
        private Prefix() {
            this(1, "");
        }
    }

    private static final List<Prefix> DECIMAL = List.of(
            new Prefix(Math.pow(1000, 6), "E"), // exa
            new Prefix(Math.pow(1000, 5), "P"), // peta
            new Prefix(Math.pow(1000, 4), "T"), // tera
            new Prefix(Math.pow(1000, 3), "G"), // giga
            new Prefix(Math.pow(1000, 2), "M"), // mega
            new Prefix(Math.pow(1000, 1), "k"), // kilo
            new Prefix(Math.pow(1000, 0), ""), // none
            new Prefix(Math.pow(1000, -1), "m"), // milli
            new Prefix(Math.pow(1000, -2), "μ"), // micro
            new Prefix(Math.pow(1000, -3), "n"), // nano
            new Prefix(Math.pow(1000, -4), "p"), // pico
            new Prefix(Math.pow(1000, -5), "f"), // femto
            new Prefix(Math.pow(1000, -6), "a")); // atto

    private static final List<Prefix> BINARY = List.of(
            new Prefix(Math.pow(1024, 6), "EiB"), // exbibyte
            new Prefix(Math.pow(1024, 5), "PiB"), // pebibyte
            new Prefix(Math.pow(1024, 4), "TiB"), // tebibyte
            new Prefix(Math.pow(1024, 3), "GiB"), // gibibyte
            new Prefix(Math.pow(1024, 2), "MiB"), // mebibyte
            new Prefix(Math.pow(1024, 1), "kiB"), // kibibyte
            new Prefix(Math.pow(1024, 0), "B")); // byte

    private String formatNumber(float number, List<Prefix> prefixes) {
        boolean negative = number < 0;
        number = Math.abs(number);

        // Find prefix
        Prefix prefix;
        if (prefixes.isEmpty()) prefix = new Prefix();
        else prefix = prefixes.getLast();
        for (Prefix p : prefixes) {
            if (number > p.value) {
                prefix = p;
                break;
            }
        }

        String result = formatFloat((float) (number / prefix.value)) + prefix.symbol;
        if (negative) return "-" + result;
        if (sign) return "+" + result;
        return result;
    }

    private String formatNumber(float number) {
        return formatNumber(number, List.of());
    }

    public String formatDuration(Duration duration) {
        long t = duration.toMillis();
        boolean negative = t < 0;
        t = Math.abs(t);

        long ms = t % 1000;
        long s = (t / 1000) % 60;
        long m = (t / (1000 * 60)) % 60;
        long h = (t / (1000 * 60 * 60)) % 24;
        long d = t / (1000 * 60 * 60 * 24);

        List<String> parts = new ArrayList<>();
        if (d > 0) parts.add(d + "d");
        if (h > 0) parts.add(h + "h");
        if (m > 0) parts.add(m + "m");
        if (s > 0) parts.add(s + "s");
        if (ms > 0 && parts.isEmpty()) parts.add(ms + "ms");
        if (parts.isEmpty()) parts.add("0s");

        String result = String.join(" ", parts);
        if (negative) return "-" + result;
        if (sign) return "+" + result;
        return result;
    }

    public String formatDecimal(float number) {
        return formatNumber(number, DECIMAL);
    }

    public String formatBytes(float bytes) {
        return withPrecision(0).formatNumber(Math.round(bytes), BINARY);
    }

    private String formatValue(float value, @Nullable String unit, boolean withUnit) {
        if ("s".equals(unit)) return formatDuration(Duration.ofMillis(Math.round(value * 1000)));
        if ("B".equals(unit)) return formatBytes(value);
        if ("%".equals(unit)) return formatNumber(value) + "%";
        if ("100%".equals(unit)) return formatNumber(value * 100) + "%";
        if (withUnit && unit != null) return formatDecimal(value) + unit;
        return formatDecimal(value);
    }

    public String formatValue(float value, @Nullable String unit) {
        return formatValue(value, unit, false);
    }

    public String formatValueWithUnit(float value, @Nullable String unit) {
        return formatValue(value, unit, true);
    }
}
