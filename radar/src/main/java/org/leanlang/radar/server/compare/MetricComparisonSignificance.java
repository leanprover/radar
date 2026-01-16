package org.leanlang.radar.server.compare;

import org.jspecify.annotations.Nullable;

public enum MetricComparisonSignificance {
    SMALL,
    MEDIUM,
    LARGE;

    public static @Nullable MetricComparisonSignificance fromLargeMediumSmall(
            boolean isLarge, boolean isMedium, boolean isSmall) {
        if (isLarge) return LARGE;
        if (isMedium) return MEDIUM;
        if (isSmall) return SMALL;
        return null;
    }

    public static MetricComparisonSignificance max(MetricComparisonSignificance a, MetricComparisonSignificance b) {
        if (a.compareTo(b) >= 0) return a;
        return b;
    }

    public static @Nullable MetricComparisonSignificance maxNullable(
            @Nullable MetricComparisonSignificance a, @Nullable MetricComparisonSignificance b) {
        if (a == null) return b;
        if (b == null) return a;
        return max(a, b);
    }
}
