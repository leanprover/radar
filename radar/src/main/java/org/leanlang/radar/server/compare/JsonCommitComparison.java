package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Stream;

public record JsonCommitComparison(
        @JsonProperty(required = true) boolean significant,
        @JsonProperty(required = true) List<JsonRunAnalysis> runs,
        @JsonProperty(required = true) List<JsonMetricComparison> metrics,
        @JsonProperty(required = true) List<String> warnings) {

    public Stream<JsonSignificance> runSignificances() {
        return runs.stream().flatMap(it -> it.significance().stream());
    }

    public Stream<JsonSignificance> metricSignificances() {
        return metrics.stream().flatMap(it -> it.significance().stream());
    }

    public Stream<JsonSignificance> significances() {
        return Stream.concat(runSignificances(), metricSignificances());
    }
}
