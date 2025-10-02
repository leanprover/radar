package org.leanlang.radar.server.repos.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JsonGhPull(@JsonProperty(required = true) Location head, @JsonProperty(required = true) Location base) {
    public record Location(@JsonProperty(required = true) String sha) {}
}
