package org.leanlang.radar.server.repos;

public record RepoMetricMetadata(
        int direction,
        boolean minorAppear,
        boolean majorAppear,
        boolean minorDisappear,
        boolean majorDisappear,
        boolean minorAnyDelta,
        boolean majorAnyDelta,
        float minorDeltaAmount,
        float majorDeltaAmount,
        float minorDeltaFactor,
        float majorDeltaFactor) {

    public RepoMetricMetadata() {
        this(
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY);
    }
}
