package org.leanlang.radar.server.repos;

public record RepoMetricMetadata(int direction) {
    public RepoMetricMetadata() {
        this(0);
    }
}
