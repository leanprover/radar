package org.leanlang.radar.server.repos.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JsonGhReaction(
        @JsonProperty(required = true) JsonGhUser user,
        @JsonProperty(required = true) String content) {}
