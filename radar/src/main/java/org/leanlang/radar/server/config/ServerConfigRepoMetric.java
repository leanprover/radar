package org.leanlang.radar.server.config;

import org.jspecify.annotations.Nullable;

public final class ServerConfigRepoMetric {
    public String match = "";

    public @Nullable Integer direction = null;

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
