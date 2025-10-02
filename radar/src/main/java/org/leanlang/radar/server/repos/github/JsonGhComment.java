package org.leanlang.radar.server.repos.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.net.URI;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JsonGhComment(
        @JsonProperty(required = true) long id,
        @JsonProperty(required = true) Instant createdAt,
        @JsonProperty(required = true) String body,
        @JsonProperty(required = true) User user,
        @JsonProperty(required = true) URI issueUrl,
        @JsonProperty(required = true) String authorAssociation) {

    public record User(@JsonProperty(required = true) String login, @JsonProperty(required = true) long id) {}

    public String idStr() {
        return String.valueOf(id);
    }

    private long issueNumber() {
        String path = issueUrl.getPath();
        int lastSlash = path.lastIndexOf('/');
        String numberStr = path.substring(lastSlash + 1);
        return Long.parseLong(numberStr);
    }

    public String issueNumberStr() {
        return String.valueOf(issueNumber());
    }
}
