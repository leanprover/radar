package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JsonOutputLineBatch(
        @JsonProperty(required = true) List<JsonOutputLine> lines,
        @JsonProperty(required = true) int start) {}
