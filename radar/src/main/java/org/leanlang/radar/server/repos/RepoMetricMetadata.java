package org.leanlang.radar.server.repos;

import java.util.Optional;

public record RepoMetricMetadata(
        int direction,
        Optional<String> baseCategory,
        float lowerThreshold,
        float upperThreshold,
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
                Optional.empty(),
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY,
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
