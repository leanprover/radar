package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public sealed interface JsonMessageSegment {
    @JsonTypeName("delta")
    record Delta(float amount, Optional<String> unit, int direction) implements JsonMessageSegment {}

    @JsonTypeName("deltaPercent")
    record DeltaPercent(float factor, int direction) implements JsonMessageSegment {}

    @JsonTypeName("metric")
    record Metric(String metric) implements JsonMessageSegment {}

    @JsonTypeName("text")
    record Text(String text) implements JsonMessageSegment {}
}
