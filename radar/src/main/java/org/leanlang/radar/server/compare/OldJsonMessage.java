package org.leanlang.radar.server.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// TODO Remove once old significance computation is obsolete
public record OldJsonMessage(
        @JsonProperty(required = true) OldMessageGoodness goodness,
        @JsonProperty(required = true) List<JsonMessageSegment> segments) {}
