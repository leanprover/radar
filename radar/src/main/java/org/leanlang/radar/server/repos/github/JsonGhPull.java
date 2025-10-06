package org.leanlang.radar.server.repos.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JsonGhPull(
        @JsonProperty(required = true) long id,
        @JsonProperty(required = true) int number,
        @JsonProperty(required = true) Instant createdAt,
        @JsonProperty(required = true) JsonGhUser user,
        @JsonProperty(required = true) String author_association,
        @JsonProperty(required = true) Location base,
        @JsonProperty(required = true) Location head) {

    public record Repo(@JsonProperty(required = true) String name, @JsonProperty(required = true) JsonGhUser owner) {}

    public record Location(
            @JsonProperty(required = true) String sha,
            @JsonProperty(required = true) String ref,
            @JsonProperty(required = true) Repo repo) {}
}
