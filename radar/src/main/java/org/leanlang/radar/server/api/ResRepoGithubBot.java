package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RESOLVED;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.jooq.impl.DSL;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/github-bot/")
public record ResRepoGithubBot(Repos repos) {
    public record JsonCommand(
            @JsonProperty(required = true) int pr,
            @JsonProperty(required = true) String url,
            String replyUrl,
            boolean active) {}

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
                        GITHUB_COMMAND.OWNER,
                        GITHUB_COMMAND.REPO,
                        GITHUB_COMMAND.COMMENT_ID_LONG,
                        GITHUB_COMMAND.COMMENT_ISSUE_NUMBER,
                        GITHUB_COMMAND.REPLY_ID_LONG,
                        GITHUB_COMMAND_RESOLVED.COMPLETED_TIME)
                .from(GITHUB_COMMAND
                        .join(GITHUB_COMMAND_RESOLVED)
                        .on(GITHUB_COMMAND.OWNER.eq(GITHUB_COMMAND_RESOLVED.OWNER))
                        .and(GITHUB_COMMAND.REPO.eq(GITHUB_COMMAND_RESOLVED.REPO))
                        .and(GITHUB_COMMAND.COMMENT_ID_LONG.eq(GITHUB_COMMAND_RESOLVED.COMMENT_ID_LONG)))
                .orderBy(
                        // First, all the in-progress commands, then all completed commands.
                        // Within either category sorted by comment creation time, newest first.
                        DSL.case_()
                                .when(GITHUB_COMMAND_RESOLVED.COMPLETED_TIME.isNull(), 0)
                                .else_(1)
                                .asc(),
                        GITHUB_COMMAND.COMMENT_CREATED_TIME.desc())
                .limit(50)
                .stream()
                .map(it -> {
                    String ownerName = it.value1();
                    String repoName = it.value2();
                    long id = it.value3();
                    int issueNumber = it.value4();
                    Long replyId = it.value5();
                    Instant completed = it.value6();
                    return new JsonCommand(
                            issueNumber,
                            prCommentUrl(ownerName, repoName, issueNumber, id),
                            prCommentUrl(ownerName, repoName, issueNumber, replyId),
                            completed == null);
                })
                .toList();

        return new JsonGet(commands);
    }

    private static String prCommentUrl(String owner, String repo, int prNumber, long id) {
        return "https://github.com/" + owner + "/" + repo + "/pull/" + prNumber + "#issuecomment-" + id;
    }
}
