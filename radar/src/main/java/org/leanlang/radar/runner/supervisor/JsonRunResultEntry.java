package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

public record JsonRunResultEntry(
        @JsonProperty(required = true) String metric,
        @JsonProperty(required = true) float value,
        Optional<String> unit,
        Optional<Integer> direction) {}
