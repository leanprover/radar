package org.leanlang.radar.server.config;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public final class ServerConfigRepoMetricFilter {
    public Pattern match = Pattern.compile("");
    public Integer direction = 0;

    public @Nullable Float checkDeltaPercentSmall;
    public @Nullable Float checkDeltaPercentMedium;
    public @Nullable Float checkDeltaPercentLarge;

    public @Nullable Float checkQuantileFactorSmall;
    public @Nullable Float checkQuantileFactorMedium;
    public @Nullable Float checkQuantileFactorLarge;

    public @Nullable String reduceExpectedDirectionReferenceCategory;

    public @Nullable Float reduceAbsoluteLimitsSmall;
    public @Nullable Float reduceAbsoluteLimitsMedium;
}
