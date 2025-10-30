package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RESOLVED;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_LAST_CHECKED;

import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.jooq.Condition;
import org.jooq.Result;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.Constants;
import org.leanlang.radar.Formatter;
import org.leanlang.radar.codegen.jooq.Tables;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandRecord;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandResolvedRecord;
import org.leanlang.radar.codegen.jooq.tables.records.GithubLastCheckedRecord;
import org.leanlang.radar.server.compare.CommitComparer;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.compare.JsonMessageSegment;
import org.leanlang.radar.server.compare.JsonSignificance;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.leanlang.radar.server.repos.github.JsonGhPull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GithubBotUpdater {
    private static final Logger log = LoggerFactory.getLogger(GithubBotUpdater.class);
    private static final String RADAR_URL = "https://radar.lean-lang.org/"; // TODO Configurable via config file

    private final Repo repo;
    private final Queue queue;
    private final RepoGh repoGh;

    private @Nullable Instant since = null;
    private @Nullable List<JsonGhComment> comments = null;

    public GithubBotUpdater(Repo repo, Queue queue, RepoGh repoGh) {
        this.repo = repo;
        this.queue = queue;
        this.repoGh = repoGh;
    }

    public void fetch() {
        if (since != null || comments != null)
            throw new IllegalStateException("fetch() must not be called more than once");

        log.info("Fetching GitHub commands for repo {}", repo.name());
        since = since();
        comments = searchForComments(since);
        log.info("Fetched GitHub commands for repo {}", repo.name());
    }

    public void update() {
        log.info("Updating GitHub commands for repo {}", repo.name());
        if (since != null && comments != null) addCommands(comments, since);
        executeCommands();
        updateReplies();
        log.info("Updated GitHub commands for repo {}", repo.name());
    }

    private Instant since() {
        return Optional.ofNullable(repo.db()
                        .read()
                        .dsl()
                        .selectFrom(Tables.GITHUB_LAST_CHECKED)
                        .fetchOne())
                .map(GithubLastCheckedRecord::getLastCheckedTime)
                .orElse(Instant.now());
    }

    private List<JsonGhComment> searchForComments(Instant since) {
        log.info("Searching for comments since {}", since);
        List<JsonGhComment> comments = repoGh.getComments(since);
        log.info("Found {} comment{} since {}", comments.size(), comments.size() == 1 ? "" : "s", since);

        return comments;
    }

    private Condition condGhCommandOwnerRepo() {
        return (GITHUB_COMMAND.OWNER.eq(repoGh.owner())).and(GITHUB_COMMAND.REPO.eq(repoGh.repo()));
    }

    private Condition condGhCommandOwnerRepoId(long id) {
        return condGhCommandOwnerRepo().and(GITHUB_COMMAND.COMMENT_ID_LONG.eq(id));
    }

    private Condition condGhCommandResolvedOwnerRepoId(long id) {
        return (GITHUB_COMMAND_RESOLVED.OWNER.eq(repoGh.owner()))
                .and(Tables.GITHUB_COMMAND_RESOLVED.REPO.eq(repoGh.repo()))
                .and(GITHUB_COMMAND_RESOLVED.COMMENT_ID_LONG.eq(id));
    }

    private void addCommands(List<JsonGhComment> comments, Instant since) {
        // Remove all commands from different GitHub repos in case we have switched repos.
        // Because of this transaction, in theory we wouldn't need to use the owner and repo in subsequent interactions,
        // but we still spell out the entire primary key every time, just to be on the safe side.
        repo.db().writeTransaction(ctx -> ctx.dsl()
                .deleteFrom(GITHUB_COMMAND)
                .where(condGhCommandOwnerRepo().not())
                .execute());

        for (JsonGhComment comment : comments) {
            Optional<GithubBotCommand> commandOpt = resolveCommand(comment);
            if (commandOpt.isEmpty()) continue;
            GithubBotCommand command = commandOpt.get();

            log.info(
                    "Found command {} on #{}",
                    command.json().id(),
                    command.json().issueNumber());

            GithubCommandRecord commandRecord = new GithubCommandRecord(
                    command.owner(),
                    command.repo(),
                    command.json().id(),
                    command.json().issueNumber(),
                    command.json().createdAt(),
                    command.json().user().id(),
                    command.json().user().login(),
                    command.json().authorAssociation(),
                    command.json().body(),
                    null,
                    command.replyContent(),
                    0);

            Optional<GithubCommandResolvedRecord> commandResolvedRecord = command.resolved()
                    .map(it -> new GithubCommandResolvedRecord(
                            command.owner(),
                            command.repo(),
                            command.json().id(),
                            it.json().id(),
                            it.json().number(),
                            it.json().createdAt(),
                            it.json().user().id(),
                            it.json().user().login(),
                            it.json().author_association(),
                            it.json().head().sha(),
                            it.json().head().ref(),
                            it.json().head().repo().owner().login(),
                            it.json().head().repo().name(),
                            it.json().base().sha(),
                            it.json().base().ref(),
                            it.chash(),
                            it.againstChash(),
                            Instant.now(),
                            null));

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

    private Optional<GithubBotCommand> resolveCommand(JsonGhComment comment) {
        if (!GithubBotCommand.isCommand(comment.body())) return Optional.empty();

        boolean commandAlreadyKnown = repo.db()
                .read()
                .dsl()
                .selectOne()
                .from(GITHUB_COMMAND)
                .where(condGhCommandOwnerRepoId(comment.id()))
                .fetch()
                .isNotEmpty();
        if (commandAlreadyKnown) return Optional.empty();

        Optional<JsonGhPull> pullOpt = repoGh.getPull(comment.issueNumber());
        if (pullOpt.isEmpty())
            return Optional.of(new GithubBotCommand(repoGh, comment, msgNotInPr(), Optional.empty()));
        JsonGhPull pull = pullOpt.get();
        String headChash = pull.head().sha();
        String baseChash = pull.base().sha();

        // GitHub's "base.sha" doesn't seem to correspond to the merge base.
        // Instead, I suspect it's the sha of the base branch at the time the PR was created.
        // Thus, we need to find the actual merge base commit ourselves.
        String againstChash;
        try {
            Repository plumbing = repo.git().plumbing();
            RevWalk revWalk = new RevWalk(plumbing);
            revWalk.setRevFilter(RevFilter.MERGE_BASE);
            revWalk.markStart(revWalk.parseCommit(ObjectId.fromString(headChash)));
            revWalk.markStart(revWalk.parseCommit(ObjectId.fromString(baseChash)));
            RevCommit mergeBase = revWalk.next();
            if (mergeBase == null) throw new Exception("RevWalk returned null");
            againstChash = mergeBase.name();
        } catch (Exception e) {
            log.error("Failed to find merge base between {} and {}", headChash, baseChash, e);
            return Optional.of(new GithubBotCommand(repoGh, comment, msgNoBase(), Optional.empty()));
        }

        return Optional.of(new GithubBotCommand(
                repoGh,
                comment,
                msgInProgress(headChash, againstChash),
                Optional.of(new GithubBotCommand.Resolved(pull, headChash, againstChash))));
    }

    private void executeCommands() {
        var commands = repo.db()
                .read()
                .dsl()
                .select(
                        GITHUB_COMMAND.COMMENT_ID_LONG,
                        GITHUB_COMMAND.COMMENT_AUTHOR_LOGIN,
                        GITHUB_COMMAND_RESOLVED.CHASH,
                        GITHUB_COMMAND_RESOLVED.AGAINST_CHASH)
                .from(GITHUB_COMMAND
                        .join(GITHUB_COMMAND_RESOLVED)
                        .on(GITHUB_COMMAND.OWNER.eq(GITHUB_COMMAND_RESOLVED.OWNER))
                        .and(GITHUB_COMMAND.REPO.eq(GITHUB_COMMAND_RESOLVED.REPO))
                        .and(GITHUB_COMMAND.COMMENT_ID_LONG.eq(GITHUB_COMMAND_RESOLVED.COMMENT_ID_LONG)))
                .where(condGhCommandOwnerRepo())
                .and(GITHUB_COMMAND_RESOLVED.COMPLETED_TIME.isNull())
                .fetch();

        for (var command : commands) {
            long id = command.value1();
            String authorLogin = command.value2();
            String chash = command.value3();
            String againstChash = command.value4();

            // Try to finish the benchmark for the comparison commit first,
            // so that when the user sees results, they're also immediately seeing a comparison.
            // Otherwise, they may falsely think that nothing has changed.
            // Note that the commit being compared against is usually in the history and already benchmarked anyway.
            boolean againstInQueue = queue.enqueueSoft(repo.name(), againstChash, Constants.PRIORITY_GITHUB_COMMAND);
            boolean inQueue = queue.enqueueSoft(repo.name(), chash, Constants.PRIORITY_GITHUB_COMMAND);

            if (inQueue || againstInQueue) {
                updateMessage(id, msgInProgress(chash, againstChash));
            } else {
                updateMessage(id, msgFinished(chash, againstChash, authorLogin, getUsersWhoReactedToReplyWithEye(id)));
                repo.db().writeTransaction(ctx -> ctx.dsl()
                        .update(GITHUB_COMMAND_RESOLVED)
                        .set(GITHUB_COMMAND_RESOLVED.COMPLETED_TIME, Instant.now())
                        .where(condGhCommandResolvedOwnerRepoId(id))
                        .execute());
            }
        }
    }

    private void updateMessage(long id, String newMessage) {
        repo.db().writeTransaction(ctx -> {
            GithubCommandRecord command = ctx.dsl()
                    .selectFrom(GITHUB_COMMAND)
                    .where(condGhCommandOwnerRepoId(id))
                    .fetchOne();
            if (command == null) return;
            if (command.getReplyContent().equals(newMessage)) return;
            command.setReplyContent(newMessage);
            command.setReplyTries(0);
            ctx.dsl().batchUpdate(command).execute();
        });
    }

    private List<String> getUsersWhoReactedToReplyWithEye(long commentId) {

        GithubCommandRecord command = repo.db()
                .read()
                .dsl()
                .selectFrom(GITHUB_COMMAND)
                .where(condGhCommandOwnerRepoId(commentId))
                .fetchOne();
        if (command == null) return List.of();

        Long replyId = command.getReplyIdLong();
        if (replyId == null) return List.of();

        return repoGh.getReactions(replyId, "eyes").stream()
                .map(it -> it.user().login())
                .toList();
    }

    private void updateReplies() {
        Result<GithubCommandRecord> commands = repo.db()
                .read()
                .dsl()
                .selectFrom(GITHUB_COMMAND)
                .where(GITHUB_COMMAND.REPLY_CONTENT.isNotNull())
                .and(GITHUB_COMMAND.REPLY_TRIES.isNotNull())
                .and(GITHUB_COMMAND.REPLY_TRIES.lt(Constants.GITHUB_MAX_TRIES))
                .fetch();

        for (GithubCommandRecord command : commands) {
            updateReply(command);
        }
    }

    private void updateReply(GithubCommandRecord command) {
        long id = command.getCommentIdLong();
        int issueNumber = command.getCommentIssueNumber();
        Long replyId = command.getReplyIdLong();
        String replyContent = command.getReplyContent();
        Integer replyTries = command.getReplyTries();

        // When updating the DB, make sure nothing important changed since the last fetch.
        var condition = condGhCommandOwnerRepo()
                .and(GITHUB_COMMAND.COMMENT_ID_LONG.eq(id))
                .and(replyId == null ? GITHUB_COMMAND.REPLY_ID_LONG.isNull() : GITHUB_COMMAND.REPLY_ID_LONG.eq(replyId))
                .and(GITHUB_COMMAND.REPLY_CONTENT.eq(replyContent))
                .and(
                        replyTries == null
                                ? GITHUB_COMMAND.REPLY_TRIES.isNull()
                                : GITHUB_COMMAND.REPLY_TRIES.eq(replyTries));

        try {
            if (replyId == null) {
                log.info("Replying to {} in #{} (try {})", id, issueNumber, replyTries);
                JsonGhComment reply = repoGh.postComment(issueNumber, replyContent);
                repo.db().writeTransaction(ctx -> ctx.dsl()
                        .update(GITHUB_COMMAND)
                        .set(GITHUB_COMMAND.REPLY_ID_LONG, reply.id())
                        .set(GITHUB_COMMAND.REPLY_TRIES, (Integer) null)
                        .where(condition)
                        .execute());

            } else {
                log.info("Updating reply {} to {} in {} (try {})", replyId, id, issueNumber, replyTries);
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
                    .set(GITHUB_COMMAND.REPLY_ID_LONG, (Long) null)
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

    private String msgNoBase() {
        return "Failed to find a commit to compare against.";
    }

    private String radarLinkToCommit(String chash) {
        return RADAR_URL + "repos/" + repo.name() + "/commits/" + chash;
    }

    private String msgInProgress(String headChash, String baseChash) {
        return "Benchmarking " + (headChash + " ([status](" + radarLinkToCommit(headChash) + "))")
                + " against "
                + (baseChash + " ([status](" + radarLinkToCommit(baseChash) + "))")
                + ".\n\n"
                + "<sub>React with :eyes: to be notified when the results are in."
                + " The command author is always notified.</sub>";
    }

    private String radarLinkToComparison(String first, String second) {
        return RADAR_URL + "repos/" + repo.name() + "/commits/" + second + "?parent=" + first;
    }

    private String msgFinished(
            String headChash, String baseChash, @Nullable String userLogin, List<String> usersThatReactedWithEye) {

        StringBuilder sb = new StringBuilder();

        sb.append("[Benchmark results](")
                .append(radarLinkToComparison(baseChash, headChash))
                .append(") for ")
                .append(headChash)
                .append(" against ")
                .append(baseChash)
                .append(" are in!");

        Stream.concat(Optional.ofNullable(userLogin).stream(), usersThatReactedWithEye.stream())
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .forEach(it -> sb.append(" @").append(it));

        JsonCommitComparison comparison = CommitComparer.compareCommits(repo, baseChash, headChash);

        List<List<JsonMessageSegment>> significantRuns =
                comparison.runSignificances().map(JsonSignificance::message).toList();
        List<List<JsonMessageSegment>> significantMajorMetrics = comparison
                .metricSignificances()
                .filter(JsonSignificance::major)
                .map(JsonSignificance::message)
                .toList();
        List<List<JsonMessageSegment>> significantMinorMetrics = comparison
                .metricSignificances()
                .filter(it -> !it.major())
                .map(JsonSignificance::message)
                .toList();

        formatSignificanceSection(sb, "Runs", true, significantRuns);
        formatSignificanceSection(sb, "Major changes", true, significantMajorMetrics);
        formatSignificanceSection(sb, "Minor changes", false, significantMinorMetrics);

        return sb.toString();
    }

    private void formatSignificanceSection(
            StringBuilder sb, String name, boolean open, List<List<JsonMessageSegment>> messages) {

        if (messages.isEmpty()) return;
        sb.append("\n");

        if (open) sb.append("<details open>\n");
        else sb.append("<details>\n");

        sb.append("<summary>")
                .append(name)
                .append(" (")
                .append(messages.size())
                .append(")")
                .append("</summary>\n");

        // If there's no empty line between the <summary> and the list, GitHub won't render it correctly.
        sb.append("\n");

        for (List<JsonMessageSegment> message : messages) {
            sb.append("- ");
            formatMessage(sb, message);
            sb.append("\n");
        }

        sb.append("</details>");
    }

    private void formatMessage(StringBuilder sb, List<JsonMessageSegment> message) {
        for (JsonMessageSegment segment : message) {
            formatMessageSegment(sb, segment);
        }
    }

    public static void formatMessageSegment(StringBuilder sb, JsonMessageSegment segment) {
        Formatter fmt = new Formatter().withSign(true);
        switch (segment) {
            case JsonMessageSegment.Delta it:
                sb.append("**")
                        .append(fmt.formatValueWithUnit(it.amount(), it.unit().orElse(null)))
                        .append("**");
                if (it.amount() * it.direction() > 0) sb.append(" (✅)");
                else if (it.amount() * it.direction() < 0) sb.append(" (\uD83D\uDFE5)");
                break;
            case JsonMessageSegment.DeltaPercent it:
                sb.append("**").append(fmt.formatValue(it.factor(), "100%")).append("**");
                if (it.factor() * it.direction() > 0) sb.append(" (✅)");
                else if (it.factor() * it.direction() < 0) sb.append(" (\uD83D\uDFE5)");
                break;
            case JsonMessageSegment.ExitCode it:
                sb.append("**").append(it.exitCode()).append("**");
                if (it.exitCode() == 0) sb.append(" (✅)");
                else sb.append(" (\uD83D\uDFE5)");
                break;
            case JsonMessageSegment.Metric it:
                sb.append("`").append(it.metric()).append("`");
                break;
            case JsonMessageSegment.Run it:
                sb.append("`").append(it.run()).append("`");
                break;
            case JsonMessageSegment.Text it:
                sb.append(it.text());
                break;
        }
    }
}
