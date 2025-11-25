package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JsonSignificance(
        @JsonProperty(required = true) boolean major, @JsonProperty(required = true) JsonMessage message) {}
