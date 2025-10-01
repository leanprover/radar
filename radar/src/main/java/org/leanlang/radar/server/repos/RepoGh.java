package org.leanlang.radar.server.repos;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.leanlang.radar.server.repos.github.JsonGhComment;

// Sadly, https://github.com/hub4j/github-api can't query all issue comments, only comments for a specific issue.
// Thus, we need to call the relevant API ourselves.
// While I'm already doing that, I'm also calling the other APIs directly because it's little overhead.

public final class RepoGh {
    private final Client client;
    private final String owner;
    private final String repo;
    private final String pat;

    public RepoGh(Client client, String owner, String repo, String pat) {
        this.client = client;
        this.owner = owner;
        this.repo = repo;
        this.pat = pat;
    }

    private UriBuilder apiUriBuilder() {
        return UriBuilder.fromUri("https://api.github.com/");
    }

    // https://docs.github.com/en/rest/issues/comments?apiVersion=2022-11-28#list-issue-comments-for-a-repository
    private List<JsonGhComment> getCommentsPage(Instant since, int page) {
        URI uri = apiUriBuilder()
                .path("repos")
                .path(owner)
                .path(repo)
                .path("issues")
                .path("comments")
                .queryParam("sort", "created")
                // By choosing this direction and iterating in increasing page order,
                // we might get some comments twice if new comments get posted while we're iterating,
                // but we won't miss any (unless more than 100 comments are posted in-between two page requests).
                .queryParam("direction", "desc")
                .queryParam("since", DateTimeFormatter.ISO_INSTANT.format(since))
                .queryParam("page", page)
                .queryParam("per_page", 100) // The maximum
                .build();

        return Arrays.asList(
                client.target(uri).request(MediaType.APPLICATION_JSON_TYPE).get(JsonGhComment[].class));
    }

    public List<JsonGhComment> getComments(Instant since) {
        List<JsonGhComment> result = new ArrayList<>();
        for (int page = 1; true; page++) {
            List<JsonGhComment> comments = getCommentsPage(since, page);
            if (comments.isEmpty()) break;
            result.addAll(comments);
        }
        return result;
    }
}
