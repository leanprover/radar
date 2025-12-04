package org.leanlang.radar.server.repos.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.net.URI;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JsonGhComment(
        @JsonProperty(required = true) long id,
        @JsonProperty(required = true) URI issueUrl,
        @JsonProperty(required = true) Instant createdAt,
        @JsonProperty(required = true) Instant updatedAt,
        @JsonProperty(required = true) JsonGhUser user,
        @JsonProperty(required = true) String authorAssociation,
        @Nullable JsonGhCommentPullRequest pullRequest,
        @JsonProperty(required = true) String body) {

    record JsonGhCommentPullRequest() {}

    public int issueNumber() {
        String path = issueUrl.getPath();
        int lastSlash = path.lastIndexOf('/');
        String numberStr = path.substring(lastSlash + 1);
        return Integer.parseInt(numberStr);
    }

    public boolean isPullRequest() {
        return pullRequest != null;
    }
}
