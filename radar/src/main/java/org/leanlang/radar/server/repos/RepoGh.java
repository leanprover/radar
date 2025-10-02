package org.leanlang.radar.server.repos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.leanlang.radar.server.repos.github.JsonGhPull;

// Sadly, https://github.com/hub4j/github-api can't query all issue comments, only comments for a specific issue.
// Thus, we need to call the relevant API ourselves.
// While I'm already doing that, I'm also calling the other APIs directly because it's little overhead.

public final class RepoGh {
    public static final int PER_PAGE = 100; // The maximum value
    public static final String API_URL = "https://api.github.com/";

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

    public String ownerAndRepo() {
        return owner + "/" + repo;
    }

    private MultivaluedMap<String, Object> headers() {
        MultivaluedHashMap<String, Object> result = new MultivaluedHashMap<>();
        result.add("Authorization", "Bearer " + pat);
        result.add("X-GitHub-Api-Version", "2022-11-28");
        return result;
    }

    // https://docs.github.com/en/rest/issues/comments?apiVersion=2022-11-28#list-issue-comments-for-a-repository
    private List<JsonGhComment> getCommentsPage(Instant since, int page, int perPage) {
        JsonGhComment[] comments = client.target(API_URL)
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
                .queryParam("per_page", perPage) // The maximum
                .request(MediaType.APPLICATION_JSON_TYPE)
                .headers(headers())
                .get(JsonGhComment[].class);

        return Arrays.asList(comments).reversed(); // Should be old to new, not new to old
    }

    public List<JsonGhComment> getComments(Instant since) {
        List<JsonGhComment> result = new ArrayList<>();
        for (int page = 1; true; page++) {
            List<JsonGhComment> comments = getCommentsPage(since, page, PER_PAGE);
            result.addAll(comments);
            if (comments.size() < PER_PAGE) break;
        }
        return result;
    }

    // https://docs.github.com/en/rest/pulls/pulls?apiVersion=2022-11-28#get-a-pull-request
    public Optional<JsonGhPull> getPull(String number) {
        try {
            JsonGhPull pull = client.target(API_URL)
                    .path("repos")
                    .path(owner)
                    .path(repo)
                    .path("pulls")
                    .path(number)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .headers(headers())
                    .get(JsonGhPull.class);
            return Optional.of(pull);
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private record JsonPostCommentData(@JsonProperty(required = true) String body) {}

    // https://docs.github.com/en/rest/issues/comments?apiVersion=2022-11-28#create-an-issue-comment
    public JsonGhComment postComment(String prNumber, String content) {
        return client.target(API_URL)
                .path("repos")
                .path(owner)
                .path(repo)
                .path("issues")
                .path(prNumber)
                .path("comments")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .headers(headers())
                .post(Entity.json(new JsonPostCommentData(content)), JsonGhComment.class);
    }

    // https://docs.github.com/en/rest/issues/comments?apiVersion=2022-11-28#update-an-issue-comment
    public void updateComment(String id, String content) {
        client.target(API_URL)
                .path("repos")
                .path(owner)
                .path(repo)
                .path("issues")
                .path("comments")
                .path(id)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .headers(headers())
                .post(Entity.json(new JsonPostCommentData(content)), JsonGhComment.class);
    }
}
