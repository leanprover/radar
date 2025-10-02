package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RESOLVED;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_LAST_CHECKED;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jooq.Result;
import org.leanlang.radar.Constants;
import org.leanlang.radar.codegen.jooq.Tables;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandRecord;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandResolvedRecord;
import org.leanlang.radar.codegen.jooq.tables.records.GithubLastCheckedRecord;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.leanlang.radar.server.repos.github.JsonGhPull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GhUpdater(Repo repo, Queue queue, RepoGh repoGh) {
    private static final Logger log = LoggerFactory.getLogger(GhUpdater.class);

    public List<JsonGhComment> searchForComments() {
        Instant since = Optional.ofNullable(repo.db()
                        .read()
                        .dsl()
                        .selectFrom(Tables.GITHUB_LAST_CHECKED)
                        .fetchOne())
                .map(GithubLastCheckedRecord::getLastCheckedTime)
                .orElse(Instant.now());

        log.info("Searching for comments since {}", since);
        List<JsonGhComment> comments = repoGh.getComments(since);
        log.info("Found {} comment{} since {}", comments.size(), comments.size() == 1 ? "" : "s", since);

        return comments;
    }

    public void addCommands(List<JsonGhComment> comments) {
        // Remove all commands from different GitHub repos in case we have switched repos.
        repo.db().writeTransaction(ctx -> ctx.dsl()
                .deleteFrom(GITHUB_COMMAND)
                .where(GITHUB_COMMAND.REPO.ne(repoGh.name()))
                .execute());

        for (JsonGhComment comment : comments) {
            Optional<GhCommand> commandOpt = resolveCommand(comment);
            if (commandOpt.isEmpty()) continue;
            GhCommand command = commandOpt.get();

            log.info("Found command {} on #{}", command.id(), command.prNumber());
            GithubCommandRecord commandRecord = new GithubCommandRecord(
                    command.repo(), command.id(), command.prNumber(), null, command.replyContent(), 0);
            Optional<GithubCommandResolvedRecord> commandResolvedRecord = command.resolved()
                    .map(it -> new GithubCommandResolvedRecord(
                            command.repo(), command.id(), it.headChash(), it.baseChash(), 1));
            repo.db().writeTransaction(ctx -> {
                ctx.dsl().batchInsert(commandRecord).execute();
                commandResolvedRecord.ifPresent(it -> ctx.dsl().batchInsert(it).execute());
            });
        }

        // Update last checked table to ensure we don't unnecessarily request too many comments
        if (!comments.isEmpty()) {
            Instant lastSeen = comments.getLast().createdAt();
            log.info("Updating last seen time to {}", lastSeen);
            repo.db().writeTransaction(ctx -> {
                ctx.dsl().deleteFrom(GITHUB_LAST_CHECKED).execute();
                ctx.dsl()
                        .insertInto(GITHUB_LAST_CHECKED, GITHUB_LAST_CHECKED.LAST_CHECKED_TIME)
                        .values(lastSeen)
                        .execute();
            });
        }
    }

    private Optional<GhCommand> resolveCommand(JsonGhComment comment) {
        String body = comment.body().strip();
        if (!(body.equals("!bench") || body.equals("!radar"))) return Optional.empty();

        boolean commandAlreadyKnown = repo.db()
                .read()
                .dsl()
                .selectOne()
                .from(GITHUB_COMMAND)
                .where(GITHUB_COMMAND.REPO.eq(repoGh.name()))
                .and(GITHUB_COMMAND.ID.eq(comment.idStr()))
                .fetch()
                .isNotEmpty();
        if (commandAlreadyKnown) return Optional.empty();

        Optional<JsonGhPull> pullOpt = repoGh.getPull(comment.issueNumberStr());
        if (pullOpt.isEmpty())
            return Optional.of(new GhCommand(
                    repoGh.name(), comment.idStr(), comment.issueNumberStr(), msgNotInPr(), Optional.empty()));
        JsonGhPull pull = pullOpt.get();

        return Optional.of(new GhCommand(
                repoGh.name(),
                comment.idStr(),
                comment.issueNumberStr(),
                msgRegistered(),
                Optional.of(
                        new GhCommand.Resolved(pull.head().sha(), pull.base().sha()))));
    }

    public void executeCommands() {
        Result<GithubCommandResolvedRecord> commands = repo.db()
                .read()
                .dsl()
                .selectFrom(GITHUB_COMMAND_RESOLVED)
                .where(GITHUB_COMMAND_RESOLVED.ACTIVE.ne(0))
                .fetch();

        for (GithubCommandResolvedRecord command : commands) {
            String headChash = command.getHeadChash();
            String baseChash = command.getBaseChash();

            boolean baseInQueue = queue.enqueueSoft(repo.name(), baseChash, Constants.PRIORITY_GITHUB_COMMAND);
            boolean headInQueue = queue.enqueueSoft(repo.name(), headChash, Constants.PRIORITY_GITHUB_COMMAND);

            if (baseInQueue || headInQueue) {
                updateMessage(command.getId(), msgInProgress(headChash, baseChash));
            } else {
                updateMessage(command.getId(), msgFinished(headChash, baseChash));
                repo.db().writeTransaction(ctx -> ctx.dsl()
                        .update(GITHUB_COMMAND_RESOLVED)
                        .set(GITHUB_COMMAND_RESOLVED.ACTIVE, 0)
                        .execute());
            }
        }
    }

    private void updateMessage(String id, String newMessage) {
        repo.db().writeTransaction(ctx -> {
            GithubCommandRecord command = ctx.dsl()
                    .selectFrom(GITHUB_COMMAND)
                    .where(GITHUB_COMMAND.REPO.eq(repoGh.name()))
                    .and(GITHUB_COMMAND.ID.eq(id))
                    .fetchOne();
            if (command == null) return;
            if (command.getReplyContent().equals(newMessage)) return;
            command.setReplyContent(newMessage);
            command.setReplyTries(0);
            ctx.dsl().batchUpdate(command).execute();
        });
    }

    public void updateReplies() {
        // TODO Implement
    }

    /*
     * Messages
     */

    private String msgNotInPr() {
        return "This command can only be used in pull requests.";
    }

    private String msgRegistered() {
        return "Command registered. Please stand by for updates.";
    }

    private String radarLinkToCommit(String chash) {
        return "https://radar.lean-lang.org/repos/" + repo.name() + "/commits/" + chash;
    }

    private String msgInProgress(String headChash, String baseChash) {
        return "Benchmarking in progress."
                + ("\n- Commit " + headChash + " ([status](" + radarLinkToCommit(headChash) + "))")
                + ("\n- Against " + baseChash + " ([status](" + radarLinkToCommit(baseChash) + "))");
    }

    private String radarLinkToComparison(String first, String second) {
        return "https://radar.lean-lang.org/repos/" + repo.name() + "/commits/" + second + "?parent=" + first;
    }

    private String msgFinished(String headChash, String baseChash) {
        return "Benchmarking finished, [results](" + radarLinkToComparison(baseChash, headChash) + ").";
    }
}
