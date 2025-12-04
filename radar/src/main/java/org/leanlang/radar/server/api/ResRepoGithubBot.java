package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RUNNING;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

@Path("/repos/{repo}/github-bot/")
public record ResRepoGithubBot(Repos repos) {
    public record JsonCommand(
            @JsonProperty(required = true) int pr,

            @Nullable String inRepo,
            @JsonProperty(required = true) String chashFirst,
            @JsonProperty(required = true) String chashSecond,

            @JsonProperty(required = true) String url,
            @Nullable String replyUrl,

            @JsonProperty(required = true) Instant created,
            @Nullable Instant completed) {}

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
                        GITHUB_COMMAND.NUMBER,
                        GITHUB_COMMAND.COMMAND_ID_LONG,
                        GITHUB_COMMAND.COMMAND_CREATED_TIME,
                        GITHUB_COMMAND.REPLY_ID_LONG,
                        GITHUB_COMMAND_RUNNING.IN_REPO,
                        GITHUB_COMMAND_RUNNING.CHASH_FIRST,
                        GITHUB_COMMAND_RUNNING.CHASH_SECOND,
                        GITHUB_COMMAND_RUNNING.COMPLETED_TIME)
                .from(GITHUB_COMMAND
                        .join(GITHUB_COMMAND_RUNNING)
                        .on(GITHUB_COMMAND.OWNER.eq(GITHUB_COMMAND_RUNNING.OWNER))
                        .and(GITHUB_COMMAND.REPO.eq(GITHUB_COMMAND_RUNNING.REPO))
                        .and(GITHUB_COMMAND.COMMAND_ID_LONG.eq(GITHUB_COMMAND_RUNNING.COMMAND_ID_LONG)))
                .orderBy(
                        // First, all the in-progress commands, then all completed commands.
                        // Within either category sorted by comment creation time, newest first.
                        DSL.case_()
                                .when(GITHUB_COMMAND_RUNNING.COMPLETED_TIME.isNull(), 0)
                                .else_(1)
                                .asc(),
                        GITHUB_COMMAND.COMMAND_CREATED_TIME.desc())
                .limit(32)
                .stream()
                .map(it -> {
                    String ownerName = it.value1();
                    String repoName = it.value2();
                    int number = it.value3();
                    long commandId = it.value4();
                    Instant created = it.value5();
                    Long replyId = it.value6();
                    String inRepo = it.value7();
                    String chashFirst = it.value8();
                    String chashSecond = it.value9();
                    Instant completed = it.value10();
                    return new JsonCommand(
                            number,
                            inRepo,
                            chashFirst,
                            chashSecond,
                            prCommentUrl(ownerName, repoName, number, commandId),
                            replyId == null ? null : prCommentUrl(ownerName, repoName, number, replyId),
                            created,
                            completed);
                })
                .toList();

        return new JsonGet(commands);
    }

    private static String prCommentUrl(String owner, String repo, int prNumber, long id) {
        return "https://github.com/" + owner + "/" + repo + "/pull/" + prNumber + "#issuecomment-" + id;
    }
}
