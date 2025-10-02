package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RESOLVED;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_LAST_CHECKED;

import jakarta.ws.rs.NotFoundException;
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
    private static final String RADAR_URL = "https://radar.lean-lang.org/"; // TODO Configurable via config file

    public Instant since() {
        return Optional.ofNullable(repo.db()
                        .read()
                        .dsl()
                        .selectFrom(Tables.GITHUB_LAST_CHECKED)
                        .fetchOne())
                .map(GithubLastCheckedRecord::getLastCheckedTime)
                .orElse(Instant.now());
    }

    public List<JsonGhComment> searchForComments(Instant since) {
        log.info("Searching for comments since {}", since);
        List<JsonGhComment> comments = repoGh.getComments(since);
        log.info("Found {} comment{} since {}", comments.size(), comments.size() == 1 ? "" : "s", since);

        return comments;
    }

    public void addCommands(List<JsonGhComment> comments, Instant since) {
        // Remove all commands from different GitHub repos in case we have switched repos.
        repo.db().writeTransaction(ctx -> ctx.dsl()
                .deleteFrom(GITHUB_COMMAND)
                .where(GITHUB_COMMAND.OWNER_AND_REPO.ne(repoGh.ownerAndRepo()))
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
        Instant lastSeen;
        if (comments.isEmpty()) lastSeen = since;
        else lastSeen = comments.getLast().createdAt();
        log.info("Updating last seen time to {}", lastSeen);
        repo.db().writeTransaction(ctx -> {
            ctx.dsl().deleteFrom(GITHUB_LAST_CHECKED).execute();
            ctx.dsl()
                    .insertInto(GITHUB_LAST_CHECKED, GITHUB_LAST_CHECKED.LAST_CHECKED_TIME)
                    .values(lastSeen)
                    .execute();
        });
    }

    private Optional<GhCommand> resolveCommand(JsonGhComment comment) {
        String body = comment.body().strip();
        if (!(body.equals("!bench") || body.equals("!radar"))) return Optional.empty();

        boolean commandAlreadyKnown = repo.db()
                .read()
                .dsl()
                .selectOne()
                .from(GITHUB_COMMAND)
                .where(GITHUB_COMMAND.OWNER_AND_REPO.eq(repoGh.ownerAndRepo()))
                .and(GITHUB_COMMAND.ID.eq(comment.idStr()))
                .fetch()
                .isNotEmpty();
        if (commandAlreadyKnown) return Optional.empty();

        Optional<JsonGhPull> pullOpt = repoGh.getPull(comment.issueNumberStr());
        if (pullOpt.isEmpty())
            return Optional.of(new GhCommand(
                    repoGh.ownerAndRepo(), comment.idStr(), comment.issueNumberStr(), msgNotInPr(), Optional.empty()));
        JsonGhPull pull = pullOpt.get();

        String headChash = pull.head().sha();
        String baseChash = pull.base().sha();
        return Optional.of(new GhCommand(
                repoGh.ownerAndRepo(),
                comment.idStr(),
                comment.issueNumberStr(),
                msgInProgress(headChash, baseChash),
                Optional.of(new GhCommand.Resolved(headChash, baseChash))));
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
                    .where(GITHUB_COMMAND.OWNER_AND_REPO.eq(repoGh.ownerAndRepo()))
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
        Result<GithubCommandRecord> commands = repo.db()
                .read()
                .dsl()
                .selectFrom(GITHUB_COMMAND)
                .where(GITHUB_COMMAND.REPLY_TRIES.lt(Constants.GITHUB_MAX_TRIES))
                .fetch();

        for (GithubCommandRecord command : commands) {
            updateReply(command);
        }
    }

    private void updateReply(GithubCommandRecord command) {
        String id = command.getId();
        String prNumber = command.getPrNumber();
        String replyId = command.getReplyId();
        String replyContent = command.getReplyContent();
        Integer replyTries = command.getReplyTries();

        // When updating the DB, make sure nothing important changed since the last fetch.
        var condition = (GITHUB_COMMAND.OWNER_AND_REPO.eq(repoGh.ownerAndRepo()))
                .and(GITHUB_COMMAND.ID.eq(id))
                .and(replyId == null ? GITHUB_COMMAND.REPLY_ID.isNull() : GITHUB_COMMAND.REPLY_ID.eq(replyId))
                .and(GITHUB_COMMAND.REPLY_CONTENT.eq(replyContent))
                .and(GITHUB_COMMAND.REPLY_TRIES.eq(replyTries));

        try {
            if (replyId == null) {
                log.info("Replying to {} in #{} (try {})", id, prNumber, replyTries);
                JsonGhComment reply = repoGh.postComment(prNumber, replyContent);
                repo.db().writeTransaction(ctx -> ctx.dsl()
                        .update(GITHUB_COMMAND)
                        .set(GITHUB_COMMAND.REPLY_TRIES, (Integer) null)
                        .set(GITHUB_COMMAND.REPLY_ID, reply.idStr())
                        .where(condition)
                        .execute());

            } else {
                log.info("Updating reply {} to {} in {} (try {})", replyId, id, prNumber, replyTries);
                repoGh.updateComment(replyId, replyContent);
                repo.db().writeTransaction(ctx -> ctx.dsl()
                        .update(GITHUB_COMMAND)
                        .set(GITHUB_COMMAND.REPLY_TRIES, (Integer) null)
                        .where(condition)
                        .execute());
            }
        } catch (NotFoundException e) {
            // Presumably our reply was deleted or something, so let's just send a new one instead the next time.
            log.error("Reply failed because of 404", e);
            repo.db().writeTransaction(ctx -> ctx.dsl()
                    .update(GITHUB_COMMAND)
                    .set(GITHUB_COMMAND.REPLY_TRIES, GITHUB_COMMAND.REPLY_TRIES.add(1))
                    .set(GITHUB_COMMAND.REPLY_ID, (String) null)
                    .where(condition)
                    .execute());
        } catch (Exception e) {
            log.error("Reply failed", e);
            repo.db().writeTransaction(ctx -> ctx.dsl()
                    .update(GITHUB_COMMAND)
                    .set(GITHUB_COMMAND.REPLY_TRIES, GITHUB_COMMAND.REPLY_TRIES.add(1))
                    .where(condition)
                    .execute());
        }
    }

    /*
     * Messages
     */

    private String msgNotInPr() {
        return "This command can only be used in pull requests.";
    }

    private String radarLinkToCommit(String chash) {
        return RADAR_URL + "repos/" + repo.name() + "/commits/" + chash;
    }

    private String msgInProgress(String headChash, String baseChash) {
        return "Benchmarking in progress."
                + ("\n- Commit " + headChash + " ([status](" + radarLinkToCommit(headChash) + "))")
                + ("\n- Against " + baseChash + " ([status](" + radarLinkToCommit(baseChash) + "))");
    }

    private String radarLinkToComparison(String first, String second) {
        return RADAR_URL + "repos/" + repo.name() + "/commits/" + second + "?parent=" + first;
    }

    private String msgFinished(String headChash, String baseChash) {
        return "Benchmarking finished, [results](" + radarLinkToComparison(baseChash, headChash) + ").";
    }
}
