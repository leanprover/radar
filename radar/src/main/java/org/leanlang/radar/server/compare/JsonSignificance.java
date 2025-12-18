package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JsonSignificance(
        @JsonProperty(required = true) int importance, // 0=small, 1=medium, 2=large
        @JsonProperty(required = true) int goodness, // <0=bad, 0=neutral, >0=good
        @JsonProperty(required = true) List<JsonMessageSegment> segments) {

    public static final int IMPORTANCE_SMALL = 0;
    public static final int IMPORTANCE_MEDIUM = 1;
    public static final int IMPORTANCE_LARGE = 2;

    public static final int GOODNESS_BAD = -1;
    public static final int GOODNESS_NEUTRAL = 0;
    public static final int GOODNESS_GOOD = 1;

    public JsonSignificance withImportance(int newImportance) {
        return new JsonSignificance(newImportance, this.goodness, this.segments);
    }
}
