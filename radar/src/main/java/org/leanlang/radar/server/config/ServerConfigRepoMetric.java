package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import org.leanlang.radar.server.repos.RepoMetricMetadata;

public final class ServerConfigRepoMetric {
    @NotEmpty
    @JsonProperty(required = true)
    public String match;

    public int direction = new RepoMetricMetadata().direction();
}
