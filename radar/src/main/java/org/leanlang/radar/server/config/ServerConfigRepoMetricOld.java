package org.leanlang.radar.server.config;

import org.jspecify.annotations.Nullable;

// TODO Remove once old significance computation is obsolete
public final class ServerConfigRepoMetricOld {
    public String match = "";

    public @Nullable Integer direction = null;

    // Estimate expected delta based on this metric to compare the actual values against.
    public @Nullable String baseCategory = null;

    // Don't make a metric significant if it is below the lower or above the upper threshold.
    public @Nullable Float lowerThreshold = null;
    public @Nullable Float upperThreshold = null;

    // Make a metric significant if it appears.
    public @Nullable Boolean minorAppear = null;
    public @Nullable Boolean majorAppear = null;

    // Make a metric significant if it disappears.
    public @Nullable Boolean minorDisappear = null;
    public @Nullable Boolean majorDisappear = null;

    // Make a metric significant if any change in value occurs.
    public @Nullable Boolean minorAnyDelta = null;
    public @Nullable Boolean majorAnyDelta = null;

    // Make a metric significant if it changed by more than this absolute amount.
    public @Nullable Float minorDeltaAmount = null;
    public @Nullable Float majorDeltaAmount = null;

    // Make a metric significant if it changed by more than this factor.
    public @Nullable Float minorDeltaFactor = null;
    public @Nullable Float majorDeltaFactor = null;
}
