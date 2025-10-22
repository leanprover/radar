package org.leanlang.radar.server.repos;

import java.util.Optional;
import java.util.regex.Pattern;
import org.leanlang.radar.server.config.ServerConfigRepoMetric;

public record RepoMetricMatcher(Pattern match, Optional<Integer> direction) {
    public RepoMetricMatcher(ServerConfigRepoMetric config) {
        this(Pattern.compile(config.match), Optional.ofNullable(config.direction));
    }

    public boolean matches(String metric) {
        return match.matcher(metric).find();
    }

    public RepoMetricMetadata update(RepoMetricMetadata metadata) {
        return new RepoMetricMetadata(direction.orElse(metadata.direction()));
    }
}
