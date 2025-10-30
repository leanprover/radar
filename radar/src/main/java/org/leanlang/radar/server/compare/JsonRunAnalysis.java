package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

public record JsonRunAnalysis(
        @JsonProperty(required = true) String name,
        @JsonProperty(required = true) String script,
        @JsonProperty(required = true) String runner,
        @JsonProperty(required = true) int exitCode,
        Optional<JsonSignificance> significance) {}
