package org.leanlang.radar.server.repos.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.net.URI;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JsonGhComment(
        @JsonProperty(required = true) long id,
        @JsonProperty(required = true) URI issueUrl,
        @JsonProperty(required = true) Instant createdAt,
        @JsonProperty(required = true) JsonGhUser user,
        @JsonProperty(required = true) String authorAssociation,
        @JsonProperty(required = true) String body) {

    public int issueNumber() {
        String path = issueUrl.getPath();
        int lastSlash = path.lastIndexOf('/');
        String numberStr = path.substring(lastSlash + 1);
        return Integer.parseInt(numberStr);
    }
}
