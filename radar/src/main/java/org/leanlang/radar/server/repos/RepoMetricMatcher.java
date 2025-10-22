package org.leanlang.radar.server.repos;

import java.util.Optional;
import java.util.regex.Pattern;
import org.leanlang.radar.server.config.ServerConfigRepoMetric;

public record RepoMetricMatcher(
        Pattern match,
        Optional<Integer> direction,
        Optional<Boolean> minorAppear,
        Optional<Boolean> majorAppear,
        Optional<Boolean> minorDisappear,
        Optional<Boolean> majorDisappear,
        Optional<Boolean> minorAnyDelta,
        Optional<Boolean> majorAnyDelta,
        Optional<Float> minorDeltaAmount,
        Optional<Float> majorDeltaAmount,
        Optional<Float> minorDeltaFactor,
        Optional<Float> majorDeltaFactor) {

    public RepoMetricMatcher(ServerConfigRepoMetric config) {
        this(
                Pattern.compile(config.match),
                Optional.ofNullable(config.direction),
                Optional.ofNullable(config.minorAppear),
                Optional.ofNullable(config.majorAppear),
                Optional.ofNullable(config.minorDisappear),
                Optional.ofNullable(config.majorDisappear),
                Optional.ofNullable(config.minorAnyDelta),
                Optional.ofNullable(config.majorAnyDelta),
                Optional.ofNullable(config.minorDeltaAmount),
                Optional.ofNullable(config.majorDeltaAmount),
                Optional.ofNullable(config.minorDeltaFactor),
                Optional.ofNullable(config.majorDeltaFactor));
    }

    public boolean matches(String metric) {
        return match.matcher(metric).find();
    }

    public RepoMetricMetadata update(RepoMetricMetadata metadata) {
        return new RepoMetricMetadata(
                direction.orElse(metadata.direction()),
                minorAppear.orElse(metadata.minorAppear()),
                majorAppear.orElse(metadata.majorAppear()),
                minorDisappear.orElse(metadata.minorDisappear()),
                majorDisappear.orElse(metadata.majorDisappear()),
                minorAnyDelta.orElse(metadata.minorAnyDelta()),
                majorAnyDelta.orElse(metadata.majorAnyDelta()),
                minorDeltaAmount.orElse(metadata.minorDeltaAmount()),
                majorDeltaAmount.orElse(metadata.majorDeltaAmount()),
                minorDeltaFactor.orElse(metadata.minorDeltaFactor()),
                majorDeltaFactor.orElse(metadata.majorDeltaFactor()));
    }
}
