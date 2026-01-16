package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JsonCommitComparison(
        @JsonProperty(required = true) boolean significant,

        @JsonProperty(required = true) List<String> warnings,
        @JsonProperty(required = true) List<JsonMessage> notes,
        @JsonProperty(required = true) List<JsonMessage> largeChanges,
        @JsonProperty(required = true) List<JsonMessage> mediumChanges,
        @JsonProperty(required = true) List<JsonMessage> smallChanges,

        @JsonProperty(required = true) List<JsonMetricComparison> measurements) {}
