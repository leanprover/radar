package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JsonMessage(
        @JsonProperty(required = true) JsonMessageGoodness goodness,
        @JsonProperty(required = true) List<JsonMessageSegment> segments) {}
