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
    public boolean refRegex = false;
    public boolean refParentsNone = false;
    public boolean refParentsFirst = false;
    public @Nullable String lakeprofReportUrl;

    // How to benchmark
    public @JsonProperty(required = true) URI benchUrl;
    public @NotEmpty @JsonProperty(required = true) String benchRef;
    public @Valid @NotEmpty @RepoRunNamesUnique @JsonProperty(required = true) List<ServerConfigRepoRun> benchRuns;

    // Significance
    public int significantLargeChanges = 1;
    public int significantMediumChanges = 5;
    public int significantSmallChanges = 20;
    public boolean significantRunFailures = true;
    public @Valid @Nullable List<ServerConfigRepoMetricFilter> significantMetrics;

    // TODO Remove once old significance computation is obsolete
    public @Valid @Nullable List<ServerConfigRepoMetricOld> oldMetrics;
    public int oldSignificantMajorMetrics = 1;
    public int oldSignificantMinorMetrics = Integer.MAX_VALUE;

    // Other platforms
    public @Valid ServerConfigRepoGithub github = new ServerConfigRepoGithub();
    public @Valid ServerConfigRepoZulip zulip = new ServerConfigRepoZulip();

    // Hacks :D
    public boolean hackMathlibCommand = false;
}
