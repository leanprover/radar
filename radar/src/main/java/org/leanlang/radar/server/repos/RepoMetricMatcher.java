package org.leanlang.radar.server.repos;

import java.util.regex.Pattern;

public record RepoMetricMatcher(Pattern pattern, RepoMetricMetadata metadata) {
    public RepoMetricMatcher(String pattern, RepoMetricMetadata metadata) {
        this(Pattern.compile(pattern), metadata);
    }

    public boolean matches(String metric) {
        return pattern.matcher(metric).find();
    }
}
