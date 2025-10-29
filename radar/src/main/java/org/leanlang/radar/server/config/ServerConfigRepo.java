package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.config.validators.RepoRunNamesUnique;

public class ServerConfigRepo {
    public @NotEmpty @JsonProperty(required = true) String name;
    public @NotEmpty @JsonProperty(required = true) String description;

    // Repo source
    public @JsonProperty(required = true) URI url;
    public @NotEmpty @JsonProperty(required = true) String ref;
    public @Nullable String lakeprofReportUrl;

    // How to benchmark
    public @JsonProperty(required = true) URI benchUrl;
    public @NotEmpty @JsonProperty(required = true) String benchRef;
    public @Valid @NotEmpty @RepoRunNamesUnique @JsonProperty(required = true) List<ServerConfigRepoRun> benchRuns;

    // Significance
    public @Valid @Nullable List<ServerConfigRepoMetric> metrics;
    public int significantMajorMetrics = 1;
    public int significantMinorMetrics = Integer.MAX_VALUE;

    // Zulip
    public @Valid @JsonProperty(required = true) ServerConfigRepoZulip zulip;
}
