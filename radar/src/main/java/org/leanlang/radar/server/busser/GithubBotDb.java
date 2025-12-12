package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RUNNING;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_LAST_CHECKED;

import java.time.Instant;
import java.util.List;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.Constants;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandRecord;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandRunningRecord;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.leanlang.radar.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record GithubBotDb(Repo repo, RepoGh repoGh) {
    private static final Logger log = LoggerFactory.getLogger(GithubBotDb.class);

    public static final int STATUS_WAITING = 0;
    public static final int STATUS_FAILED = 1;
    public static final int STATUS_SUCCEEDED = 2;

    private Condition condOwnerRepo() {
        return GITHUB_COMMAND.OWNER.eq(repoGh.owner()).and(GITHUB_COMMAND.REPO.eq(repoGh.repo()));
    }

    private Condition condOwnerRepoId(long commandId) {
        return condOwnerRepo().and(GITHUB_COMMAND.COMMAND_ID_LONG.eq(commandId));
    }

    private Condition condRunningOwnerRepoId(long commandId) {
        return (GITHUB_COMMAND_RUNNING.OWNER.eq(repoGh.owner()))
                .and(GITHUB_COMMAND_RUNNING.REPO.eq(repoGh.repo()))
                .and(GITHUB_COMMAND_RUNNING.COMMAND_ID_LONG.eq(commandId));
    }

    public Instant lastChecked() {
        return repo.db()
                .read()
                .dsl()
                .selectFrom(GITHUB_LAST_CHECKED)
                .fetchOptional(GITHUB_LAST_CHECKED.LAST_CHECKED_TIME)
                .orElse(Instant.now());
    }

    public void setLastChecked(Instant since) {
        log.info("Updating last checked time to {}", since);

        repo.db().writeTransaction(ctx -> {
            ctx.dsl().deleteFrom(GITHUB_LAST_CHECKED).execute();
            ctx.dsl()
                    .insertInto(GITHUB_LAST_CHECKED, GITHUB_LAST_CHECKED.LAST_CHECKED_TIME)
                    .values(since)
                    .execute();
        });
    }

    public void addOrUpdateCommand(JsonGhComment comment) {
        repo.db().writeTransaction(ctx -> {
            GithubCommandRecord record = ctx.dsl()
                    .selectFrom(GITHUB_COMMAND)
                    .where(condOwnerRepoId(comment.id()))
                    .fetchOne();

            if (record == null) {
                if (GithubBotCommand.isCommand(comment.body())) record = new GithubCommandRecord();
                else return;
            } else if (record.getStatus() == STATUS_SUCCEEDED) return;
            log.debug("Added or updated command {} in #{}", comment.id(), comment.issueNumber());

            record.setOwner(repoGh.owner());
            record.setRepo(repoGh.repo());
            record.setNumber(comment.issueNumber());
            record.setIsPr(comment.isPullRequest() ? 1 : 0);

            record.setCommandIdLong(comment.id());
            record.setCommandCreatedTime(comment.createdAt());
            record.setCommandUpdatedTime(comment.updatedAt());
            record.setCommandAuthorIdLong(comment.user().id());
            record.setCommandAuthorLogin(comment.user().login());
            record.setCommandAuthorAssociation(comment.authorAssociation());
            record.setCommandBody(comment.body());

            // If the command is edited, we should have a closer look at it again.
            if (record.modified(GITHUB_COMMAND.COMMAND_BODY)) record.setStatus(STATUS_WAITING);

            ctx.dsl().batchStore(record).execute();
        });
    }

    public void pruneCommandsNotFromCurrentRepo() {
        repo.db().writeTransaction(ctx -> {
            int affected = ctx.dsl()
                    .deleteFrom(GITHUB_COMMAND)
                    .where(condOwnerRepo().not())
                    .execute();
            if (affected > 0) log.info("Pruned {} command(s) not from current repo", affected);
        });
    }

    public List<GithubCommandRecord> unfinishedCommands() {
        return repo.db()
                .read()
                .dsl()
                .selectFrom(GITHUB_COMMAND)
                .where(condOwnerRepo())
                .and(GITHUB_COMMAND.STATUS.eq(STATUS_WAITING))
                .fetch();
    }

    public List<Pair<GithubCommandRecord, GithubCommandRunningRecord>> runningCommands() {
        return repo
                .db()
                .read()
                .dsl()
                .selectFrom(GITHUB_COMMAND_RUNNING
                        .join(GITHUB_COMMAND)
                        .on(GITHUB_COMMAND.OWNER.eq(GITHUB_COMMAND_RUNNING.OWNER))
                        .and(GITHUB_COMMAND.REPO.eq(GITHUB_COMMAND_RUNNING.REPO))
                        .and(GITHUB_COMMAND.COMMAND_ID_LONG.eq(GITHUB_COMMAND_RUNNING.COMMAND_ID_LONG)))
                .where(condOwnerRepo())
                .and(GITHUB_COMMAND_RUNNING.COMPLETED_TIME.isNull())
                .stream()
                .map(it -> new Pair<>(it.into(GITHUB_COMMAND), it.into(GITHUB_COMMAND_RUNNING)))
                .toList();
    }

    public List<GithubCommandRecord> commandsToUpdateRepliesOf() {
        return repo.db()
                .read()
                .dsl()
                .selectFrom(GITHUB_COMMAND)
                .where(condOwnerRepo())
                .and(GITHUB_COMMAND.REPLY_BODY.isNotNull())
                .and(GITHUB_COMMAND.REPLY_TRIES.isNotNull())
                .and(GITHUB_COMMAND.REPLY_TRIES.lt(Constants.GITHUB_MAX_TRIES))
                .fetch();
    }

    private void updateStatusAndReply(Configuration ctx, long commandId, int status, String body) {
        GithubCommandRecord command = ctx.dsl()
                .selectFrom(GITHUB_COMMAND)
                .where(condOwnerRepoId(commandId))
                .fetchOne();
        if (command == null) return;

        command.setStatus(status);

        if (!body.equals(command.getReplyBody())) {
            command.setReplyBody(body);
            command.setReplyTries(0);
        }

        ctx.dsl().batchUpdate(command).execute();
    }

    private void addOrUpdateRunning(
            Configuration ctx, long commandId, @Nullable String inRepo, String chashFirst, String chashSecond) {

        GithubCommandRunningRecord record = ctx.dsl()
                .selectFrom(GITHUB_COMMAND_RUNNING)
                .where(condRunningOwnerRepoId(commandId))
                .fetchOne();

        if (record == null) record = new GithubCommandRunningRecord();

        record.setOwner(repoGh.owner());
        record.setRepo(repoGh.repo());
        record.setCommandIdLong(commandId);

        record.setInRepo(inRepo);
        record.setChashFirst(chashFirst);
        record.setChashSecond(chashSecond);

        record.setStartedTime(Instant.now());

        ctx.dsl().batchStore(record).execute();
    }

    private void removeRunning(Configuration ctx, long commandId) {
        ctx.dsl()
                .deleteFrom(GITHUB_COMMAND_RUNNING)
                .where(condRunningOwnerRepoId(commandId))
                .execute();
    }

    public void setCommandFailed(long commandId, String reply) {
        repo.db().writeTransaction(ctx -> {
            updateStatusAndReply(ctx, commandId, STATUS_FAILED, reply);
            removeRunning(ctx, commandId);
        });
    }

    public void setCommandSucceeded(long commandId, String reply) {
        repo.db().writeTransaction(ctx -> {
            updateStatusAndReply(ctx, commandId, STATUS_SUCCEEDED, reply);
            removeRunning(ctx, commandId);
        });
    }

    public void setCommandWaiting(long commandId, String reply) {
        repo.db().writeTransaction(ctx -> {
            updateStatusAndReply(ctx, commandId, STATUS_WAITING, reply);
            removeRunning(ctx, commandId);
        });
    }

    public void setCommandRunningStarted(
            long commandId, String reply, @Nullable String inRepo, String chashFirst, String chashSecond) {

        repo.db().writeTransaction(ctx -> {
            updateStatusAndReply(ctx, commandId, STATUS_SUCCEEDED, reply);
            addOrUpdateRunning(ctx, commandId, inRepo, chashFirst, chashSecond);
        });
    }

    public void setCommandRunningUpdate(long commandId, String reply) {
        repo.db().writeTransaction(ctx -> updateStatusAndReply(ctx, commandId, STATUS_SUCCEEDED, reply));
    }

    public void setCommandRunningFinished(long commandId, String reply) {
        repo.db().writeTransaction(ctx -> {
            updateStatusAndReply(ctx, commandId, STATUS_SUCCEEDED, reply);
            ctx.dsl()
                    .update(GITHUB_COMMAND_RUNNING)
                    .set(GITHUB_COMMAND_RUNNING.COMPLETED_TIME, Instant.now())
                    .where(condRunningOwnerRepoId(commandId))
                    .execute();
        });
    }

    private Condition replyCondition(long commandId, Long replyId, String replyBody, Integer replyTries) {
        // When updating the DB, make sure nothing important changed since the last fetch.
        return (GITHUB_COMMAND.OWNER.eq(repoGh.owner()))
                .and(GITHUB_COMMAND.REPO.eq(repoGh.repo()))
                .and(GITHUB_COMMAND.COMMAND_ID_LONG.eq(commandId))
                .and(replyId == null ? GITHUB_COMMAND.REPLY_ID_LONG.isNull() : GITHUB_COMMAND.REPLY_ID_LONG.eq(replyId))
                .and(GITHUB_COMMAND.REPLY_BODY.eq(replyBody))
                .and(
                        replyTries == null
                                ? GITHUB_COMMAND.REPLY_TRIES.isNull()
                                : GITHUB_COMMAND.REPLY_TRIES.eq(replyTries));
    }

    public void replySent(long commandId, @Nullable String replyBody, @Nullable Integer replyTries, long newReplyId) {
        repo.db().writeTransaction(ctx -> ctx.dsl()
                .update(GITHUB_COMMAND)
                .set(GITHUB_COMMAND.REPLY_ID_LONG, newReplyId)
                .set(GITHUB_COMMAND.REPLY_TRIES, (Integer) null)
                .where(replyCondition(commandId, null, replyBody, replyTries))
                .execute());
    }

    public void replyUpdated(
            long commandId, @Nullable Long replyId, @Nullable String replyBody, @Nullable Integer replyTries) {
        repo.db().writeTransaction(ctx -> ctx.dsl()
                .update(GITHUB_COMMAND)
                .set(GITHUB_COMMAND.REPLY_TRIES, (Integer) null)
                .where(replyCondition(commandId, replyId, replyBody, replyTries))
                .execute());
    }

    public void replyFailed(
            long commandId, @Nullable Long replyId, @Nullable String replyBody, @Nullable Integer replyTries) {
        repo.db().writeTransaction(ctx -> ctx.dsl()
                .update(GITHUB_COMMAND)
                .set(GITHUB_COMMAND.REPLY_TRIES, GITHUB_COMMAND.REPLY_TRIES.add(1))
                .set(GITHUB_COMMAND.REPLY_ID_LONG, (Long) null)
                .where(replyCondition(commandId, replyId, replyBody, replyTries))
                .execute());
    }

    public void replyDisappeared(long commandId) {
        repo.db().writeTransaction(ctx -> ctx.dsl()
                .deleteFrom(GITHUB_COMMAND)
                .where(condOwnerRepoId(commandId))
                .execute());
    }
}
