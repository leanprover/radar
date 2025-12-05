package org.leanlang.radar.server.busser;

import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.leanlang.radar.Constants;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandRecord;
import org.leanlang.radar.codegen.jooq.tables.records.GithubCommandRunningRecord;
import org.leanlang.radar.server.compare.CommitComparer;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.Repos;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.leanlang.radar.server.repos.github.JsonGhPull;
import org.leanlang.radar.util.GithubLinker;
import org.leanlang.radar.util.Pair;
import org.leanlang.radar.util.RadarLinker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GithubBotUpdater {
    private static final Logger log = LoggerFactory.getLogger(GithubBotUpdater.class);

    private final Repos repos;
    private final Queue queue;
    private final Busser busser;
    private final Repo repo;
    private final RepoGh repoGh;

    private final GithubBotDb db;
    private final GithubBotMessages msgs;

    public GithubBotUpdater(
            RadarLinker radarLinker, Repos repos, Queue queue, Busser busser, Repo repo, RepoGh repoGh) {

        this.repos = repos;
        this.queue = queue;
        this.busser = busser;
        this.repo = repo;
        this.repoGh = repoGh;

        this.db = new GithubBotDb(repo, repoGh);
        this.msgs = new GithubBotMessages(radarLinker, new GithubLinker(repoGh.owner(), repoGh.repo()));
    }

    public void fetch() {
        log.info("Fetching GitHub commands for repo {}", repo.name());

        Instant lastChecked = db.lastChecked();
        List<JsonGhComment> comments = repoGh.getComments(lastChecked);

        for (JsonGhComment comment : comments) {
            db.addOrUpdateCommand(comment);
            if (comment.createdAt().isAfter(lastChecked)) lastChecked = comment.createdAt();
        }

        db.setLastChecked(lastChecked);

        log.info("Fetched GitHub commands for repo {}", repo.name());
    }

    public void update() {
        log.info("Updating GitHub commands for repo {}", repo.name());

        db.pruneCommandsNotFromCurrentRepo();
        for (var command : db.unfinishedCommands()) checkUnfinishedCommand(command);
        for (var pair : db.runningCommands()) executeRunningCommand(pair.left(), pair.right());
        for (var command : db.commandsToUpdateRepliesOf()) updateReply(command);

        log.info("Updated GitHub commands for repo {}", repo.name());
    }

    private void checkUnfinishedCommand(GithubCommandRecord command) {
        Long commandId = command.getCommandIdLong();
        log.debug("Checking unfinished command {} in #{}", commandId, command.getNumber());

        JsonGhPull pull = repoGh.getPull(command.getNumber()).orElse(null);
        if (pull == null) {
            // We don't need to accept edits, so STATUS_SUCCEEDED, not STATUS_FAILED.
            log.debug("Command is not in a PR.");
            db.setCommandSucceeded(commandId, msgs.notInPr());
            return;
        }

        JsonGhComment comment = repoGh.getComment(commandId).orElse(null);
        if (comment == null) {
            // The original comment is gone, so there's no need to accept edits.
            log.debug("Command is deleted.");
            db.setCommandSucceeded(commandId, msgs.deleted());
            return;
        }

        // Unconditionally updating, rather than adding.
        db.addOrUpdateCommand(comment);

        GithubBotCommand parsed = GithubBotCommand.parse(comment.body()).orElse(null);
        if (parsed == null) {
            log.debug("Message contains no command.");
            db.setCommandFailed(commandId, msgs.noLongerACommand());
            return;
        }

        switch (parsed) {
            case GithubBotCommand.TooManyCommands p -> db.setCommandFailed(commandId, msgs.tooManyCommands());
            case GithubBotCommand.Bench p -> startBenchCommand(command, pull);
            case GithubBotCommand.BenchMathlib p -> startMathlibBenchCommand(command, pull);
        }
    }

    private List<String> superfluousLabels(JsonGhPull pull) {
        Set<String> blockingLabels = new HashSet<>(repoGh.config().blockingLabels);
        return pull.labels().stream()
                .map(JsonGhPull.Label::name)
                .filter(blockingLabels::contains)
                .sorted()
                .toList();
    }

    private Optional<Pair<String, String>> findComparisonCommits(Repo repo, String base, String head) {
        // GitHub's "base.sha" doesn't seem to correspond to the merge base.
        // Instead, I suspect it's the sha of the base branch at the time the PR was created.
        // Thus, we need to find the actual merge base commit ourselves.
        try {
            Repository plumbing = repo.git().plumbing();
            RevWalk revWalk = new RevWalk(plumbing);
            revWalk.setRevFilter(RevFilter.MERGE_BASE);
            ObjectId headId = plumbing.resolve(head);
            ObjectId baseId = plumbing.resolve(base);
            revWalk.markStart(revWalk.parseCommit(headId));
            revWalk.markStart(revWalk.parseCommit(baseId));
            RevCommit mergeBase = revWalk.next();
            if (mergeBase == null) throw new Exception("RevWalk returned null");
            return Optional.of(new Pair<>(mergeBase.name(), headId.name()));
        } catch (Exception e) {
            log.error("Failed to find merge base between {} and {}", head, base, e);
            return Optional.empty();
        }
    }

    private void startBenchCommand(GithubCommandRecord command, JsonGhPull pull) {
        Long commandId = command.getCommandIdLong();
        log.info("Starting bench command for comment {} in #{}", commandId, command.getNumber());

        List<String> superfluousLabels = superfluousLabels(pull);
        if (!superfluousLabels.isEmpty()) {
            log.info("Bench command is blocked by labels {}", superfluousLabels);
            db.setCommandWaiting(commandId, msgs.labelMismatch(superfluousLabels, List.of()));
            return;
        }

        Pair<String, String> commits = findComparisonCommits(
                        repo, pull.base().sha(), pull.head().sha())
                .orElse(null);
        if (commits == null) {
            log.info("Failed to find appropriate commits for comparison");
            db.setCommandFailed(commandId, msgs.failedToFindMergeBase());
            return;
        }

        db.setCommandRunningStarted(
                commandId,
                msgs.inProgress(repo, false, commits.left(), commits.right()),
                null,
                commits.left(),
                commits.right());
    }

    private void executeRunningCommand(GithubCommandRecord command, GithubCommandRunningRecord running) {
        Long commandId = command.getCommandIdLong();

        Repo inRepo = repo;
        if (running.getInRepo() != null) inRepo = repos.repo(running.getInRepo());

        String chashFirst = running.getChashFirst();
        String chashSecond = running.getChashSecond();

        // Try to finish the benchmark for the comparison commit first,
        // so that when the user sees results, they're also immediately seeing a comparison.
        // Otherwise, they may falsely think that nothing has changed.
        // Note that the commit being compared against is usually in the history and already benchmarked anyway.
        boolean firstInQueue = queue.enqueueSoft(inRepo.name(), chashFirst, Constants.PRIORITY_GITHUB_COMMAND);
        boolean secondInQueue = queue.enqueueSoft(inRepo.name(), chashSecond, Constants.PRIORITY_GITHUB_COMMAND);
        if (firstInQueue || secondInQueue) {
            db.setCommandRunningUpdate(
                    commandId, msgs.inProgress(inRepo, !inRepo.name().equals(repo.name()), chashFirst, chashSecond));
            return;
        }

        List<String> usersThatReactedWithEye = getUsersThatReactedWithEyes(command);
        JsonCommitComparison comparison = CommitComparer.compareCommits(inRepo, chashFirst, chashSecond);

        db.setCommandRunningFinished(
                commandId,
                msgs.finished(
                        inRepo,
                        !inRepo.name().equals(repo.name()),
                        chashFirst,
                        chashSecond,
                        command.getCommandAuthorLogin(),
                        usersThatReactedWithEye,
                        comparison));
    }

    private List<String> getUsersThatReactedWithEyes(GithubCommandRecord command) {
        Long replyId = command.getReplyIdLong();
        if (replyId == null) return List.of();

        try {
            return repoGh.getEyesReactions(replyId).stream()
                    .map(it -> it.user().login())
                    .toList();
        } catch (NotFoundException e) {
            // Looks like our reply was deleted.
            log.error("Reactions retrieval failed because of 404", e);
            return List.of();
        }
    }

    private void updateReply(GithubCommandRecord command) {
        int number = command.getNumber();
        long commandId = command.getCommandIdLong();
        Long replyId = command.getReplyIdLong();
        String replyBody = command.getReplyBody();
        Integer replyTries = command.getReplyTries();

        try {
            if (replyId == null) {
                log.info("Replying to {} in #{} (try {})", commandId, number, replyTries);
                JsonGhComment reply = repoGh.postComment(number, replyBody);
                db.replySent(commandId, replyBody, replyTries, reply.id());
            } else {
                log.info("Updating reply {} to {} in {} (try {})", replyId, commandId, number, replyTries);
                repoGh.updateComment(replyId, replyBody);
                db.replyUpdated(commandId, replyId, replyBody, replyTries);
            }
        } catch (NotFoundException e) {
            // Looks like our reply was intentionally deleted, so we'll stay silent.
            log.error("Reply failed because of 404", e);
            db.replyDisappeared(commandId);
        } catch (Exception e) {
            log.error("Reply failed", e);
            db.replyFailed(commandId, replyId, replyBody, replyTries);
        }
    }

    // Hack territory

    private void startMathlibBenchCommand(GithubCommandRecord command, JsonGhPull pull) {
        Repo repoMathlib = repos.repo("mathlib4-nightly-testing");
        Long commandId = command.getCommandIdLong();
        log.info("Starting mathlib bench command for comment {} in #{}", commandId, command.getNumber());
        try {
            busser.fetchRepoCallOnlyIfYouKnowWhatYoureDoing(repoMathlib.name());
        } catch (GitAPIException e) {
            log.error("Failed to fetch mathlib", e);
            return;
        }

        Set<String> labels = pull.labels().stream().map(JsonGhPull.Label::name).collect(Collectors.toSet());
        List<String> superfluousLabels = superfluousLabels(pull);
        List<String> missingLabels = Stream.of("toolchain-available", "mathlib4-nightly-available")
                .filter(it -> !labels.contains(it))
                .sorted()
                .toList();

        if (!superfluousLabels.isEmpty() || !missingLabels.isEmpty()) {
            log.info(
                    "Mathlib bench command is blocked by labels superfluous={} missing={}",
                    superfluousLabels,
                    missingLabels);
            db.setCommandWaiting(commandId, msgs.labelMismatch(superfluousLabels, missingLabels));
            return;
        }

        String base = "nightly-testing";
        String head = "lean-pr-testing-" + pull.number();
        Pair<String, String> commits =
                findComparisonCommits(repoMathlib, base, head).orElse(null);
        if (commits == null) {
            log.info("Failed to find appropriate commits for comparison in {}", repoMathlib.name());
            db.setCommandFailed(commandId, msgs.failedToFindMergeBase());
            return;
        }

        db.setCommandRunningStarted(
                commandId,
                msgs.inProgress(repoMathlib, true, commits.left(), commits.right()),
                repoMathlib.name(),
                commits.left(),
                commits.right());
    }
}
