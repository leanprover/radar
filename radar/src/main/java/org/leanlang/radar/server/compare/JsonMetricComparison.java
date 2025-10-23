package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

public record JsonMetricComparison(
        @JsonProperty(required = true) String metric,
        Optional<Float> first,
        Optional<Float> second,
        Optional<String> firstSource,
        Optional<String> secondSource,
        Optional<String> unit,
        @JsonProperty(required = true) int direction,
        Optional<JsonMetricSignificance> significance) {}
