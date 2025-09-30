package org.leanlang.radar.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

public record ServerConfigRepoRun(
        @NotEmpty @JsonProperty(required = true) String name,
        @NotEmpty @JsonProperty(required = true) String script,
        @NotEmpty @JsonProperty(required = true) String runner) {}
