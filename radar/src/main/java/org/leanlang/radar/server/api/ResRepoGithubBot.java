package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RESOLVED;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/github-bot/")
public record ResRepoGithubBot(Repos repos) {
    public record JsonCommand(
            @JsonProperty(required = true) String pr, @JsonProperty(required = true) String url, String replyUrl) {}

    public record JsonGet(@JsonProperty(required = true) List<JsonCommand> commands) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(@PathParam("repo") String name) {
        Repo repo = repos.repo(name);

        List<JsonCommand> commands = repo
                .db()
                .read()
                .dsl()
                .select(
                        GITHUB_COMMAND.OWNER_AND_REPO,
                        GITHUB_COMMAND.PR_NUMBER,
                        GITHUB_COMMAND.ID,
                        GITHUB_COMMAND.REPLY_ID)
                .from(GITHUB_COMMAND
                        .join(GITHUB_COMMAND_RESOLVED)
                        .on(GITHUB_COMMAND.OWNER_AND_REPO.eq(GITHUB_COMMAND_RESOLVED.OWNER_AND_REPO))
                        .and(GITHUB_COMMAND.ID.eq(GITHUB_COMMAND_RESOLVED.ID)))
                .stream()
                .map(it -> {
                    String ownerAndRepo = it.value1();
                    String prNumber = it.value2();
                    String id = it.value3();
                    String replyId = it.value4();
                    return new JsonCommand(
                            prNumber,
                            prCommentUrl(ownerAndRepo, prNumber, id),
                            prCommentUrl(ownerAndRepo, prNumber, replyId));
                })
                .toList();

        return new JsonGet(commands);
    }

    private static String prCommentUrl(String ownerAndRepo, String prNumber, String id) {
        return "https://github.com/" + ownerAndRepo + "/pull/" + prNumber + "#issuecomment-" + id;
    }
}
