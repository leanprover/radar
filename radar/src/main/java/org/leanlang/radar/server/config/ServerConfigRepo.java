package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record ServerConfigRepo(
        @NotEmpty @JsonProperty(required = true) String name,
        @NotEmpty @JsonProperty(required = true) String description,
        @JsonProperty(required = true) URI url,
        @NotEmpty @JsonProperty(required = true) Set<String> track,
        @JsonProperty(required = true) URI benchUrl,
        @NotEmpty @JsonProperty(required = true) String benchRef,
        @Valid @NotEmpty @RepoRunNamesUnique @JsonProperty(required = true) List<ServerConfigRepoRun> benchRuns,
        @Valid @Nullable List<ServerConfigRepoMetric> metrics,
        @Nullable String lakeprofReportUrl) {}
