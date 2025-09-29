package org.leanlang.radar.server.queue;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.jooq.Record1;
import org.leanlang.radar.Constants;
import org.leanlang.radar.codegen.jooq.Tables;
import org.leanlang.radar.codegen.jooq.tables.records.MetricsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.QueueRecord;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.runner.supervisor.JsonJob;
import org.leanlang.radar.runner.supervisor.JsonRunResult;
import org.leanlang.radar.runner.supervisor.JsonRunResultEntry;
import org.leanlang.radar.server.config.ServerConfigRepo;
import org.leanlang.radar.server.config.ServerConfigRepoRun;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.runners.Runners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Queue(Repos repos, Runners runners) {
    private static final Logger log = LoggerFactory.getLogger(Queue.class);

    private record RunId(String repo, String chash, String name) {}

    private Map<RunId, Run.Active> getActiveRuns() {
        return runners.runners().stream()
                .flatMap(it -> it.status().stream())
                .flatMap(it -> it.activeRun().stream())
                .collect(Collectors.toUnmodifiableMap(
                        it -> new RunId(
                                it.job().repo(), it.job().chash(), it.job().name()),
                        it -> new Run.Active(it.job().benchChash(), it.startTime(), it.lines())));
    }

    private static Map<RunId, Run.Finished> getFinishedRunsForRepo(Repo repo, Configuration ctx) {
        return ctx
                .dsl()
                .select(
                        RUNS.CHASH,
                        RUNS.NAME,
                        RUNS.CHASH_BENCH,
                        RUNS.START_TIME,
                        RUNS.END_TIME,
                        RUNS.SCRIPT_START_TIME,
                        RUNS.SCRIPT_END_TIME,
                        RUNS.EXIT_CODE)
                .from(QUEUE.join(RUNS).on(RUNS.CHASH.eq(QUEUE.CHASH)))
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        it -> new RunId(repo.name(), it.value1(), it.value2()),
                        it -> new Run.Finished(
                                it.value3(),
                                it.value4(),
                                it.value5(),
                                Optional.ofNullable(it.value6()),
                                Optional.ofNullable(it.value7()),
                                it.value8())));
    }

    private static Run buildRun(
            String repo,
            String chash,
            ServerConfigRepoRun run,
            Map<RunId, Run.Active> activeRuns,
            Map<RunId, Run.Finished> finishedRuns) {
        RunId id = new RunId(repo, chash, run.name());
        return new Run(
                run.name(),
                run.script(),
                run.runner(),
                Optional.ofNullable(activeRuns.get(id)),
                Optional.ofNullable(finishedRuns.get(id)));
    }

    private static Task buildTask(
            Repo repo, QueueRecord task, Map<RunId, Run.Active> activeRuns, Map<RunId, Run.Finished> finishedRuns) {
        return new Task(
                repo,
                task.getChash(),
                task.getQueuedTime(),
                task.getBumpedTime(),
                repo.config().benchRuns().stream()
                        .map(run -> buildRun(repo.name(), task.getChash(), run, activeRuns, finishedRuns))
                        .toList());
    }

    public List<Task> getTasks() {
        Map<RunId, Run.Active> activeRuns = getActiveRuns();

        List<Task> result = new ArrayList<>();
        for (Repo repo : repos.repos()) {
            result.addAll(repo.db().readTransactionResult(ctx -> {
                Map<RunId, Run.Finished> finishedRuns = getFinishedRunsForRepo(repo, ctx);
                return ctx.dsl().selectFrom(Tables.QUEUE).stream()
                        .map(task -> Queue.buildTask(repo, task, activeRuns, finishedRuns))
                        .toList();
            }));
        }
        result.sort(Comparator.comparing(Task::bumped).reversed());
        return result;
    }

    public Optional<Task> getTask(String repoName, String chash) {
        Repo repo = repos.repo(repoName);
        return repo.db().readTransactionResult(ctx -> {
            QueueRecord record = repo.db()
                    .read()
                    .dsl()
                    .selectFrom(QUEUE)
                    .where(QUEUE.CHASH.eq(chash))
                    .fetchOne();
            if (record == null) return Optional.empty();

            Map<RunId, Run.Active> activeRuns = getActiveRuns();
            Map<RunId, Run.Finished> finishedRuns = getFinishedRunsForRepo(repo, ctx);
            return Optional.of(buildTask(repo, record, activeRuns, finishedRuns));
        });
    }

    private static void enqueueBump(Configuration ctx, QueueRecord inQueue, int priority) {
        // Bumping the position of higher-priority queue entries
        // could lead to runs being run earlier than "authorized".
        if (inQueue.getPriority() > priority) return;

        // Bump as if we had freshly inserted it into the queue
        inQueue.setPriority(priority);
        inQueue.setBumpedTime(Instant.now());
        ctx.dsl().batchUpdate(inQueue).execute();
    }

    private static void enqueueInsert(String chash, int priority, Configuration ctx) {
        Instant now = Instant.now();
        QueueRecord record = new QueueRecord(chash, now, now, priority);
        ctx.dsl().batchInsert(record).execute();

        ctx.dsl()
                .update(COMMITS)
                .set(COMMITS.SEEN, 1)
                .where(COMMITS.CHASH.eq(chash))
                .execute();
    }

    /**
     * Ensure a commit is in the queue if results are not already available,
     * bumping its position and priority if appropriate.
     */
    public void enqueueSoft(String repoName, String chash, int priority) {
        log.info("Enqueueing commit {} for repo {} (soft)", chash, repoName);
        Repo repo = repos.repo(repoName);

        repo.db().writeTransaction(ctx -> {
            boolean runsExist = !ctx.dsl()
                    .selectOne()
                    .from(RUNS)
                    .where(RUNS.CHASH.eq(chash))
                    .fetch()
                    .isEmpty();
            // No need to enqueue, we already have results.
            if (runsExist) return;

            QueueRecord inQueue =
                    ctx.dsl().selectFrom(QUEUE).where(QUEUE.CHASH.eq(chash)).fetchOne();
            if (inQueue != null) {
                enqueueBump(ctx, inQueue, priority);
                return;
            }

            // Insert into queue anew.
            // No need to delete results since there aren't any.
            enqueueInsert(chash, priority, ctx);
        });
    }

    /**
     * Add a commit to the queue, deleting all existing results
     * and bumping its position and priority if appropriate.
     */
    public void enqueueHard(String repoName, String chash, int priority) {
        log.info("Enqueueing commit {} for repo {} (hard)", chash, repoName);
        Repo repo = repos.repo(repoName);

        boolean added = repo.db().writeTransactionResult(ctx -> {
            QueueRecord inQueue =
                    ctx.dsl().selectFrom(QUEUE).where(QUEUE.CHASH.eq(chash)).fetchOne();
            if (inQueue != null) {
                enqueueBump(ctx, inQueue, priority);
                return false;
            }

            // Insert into queue anew.
            enqueueInsert(chash, priority, ctx);

            // Existing data must be removed because new data will be added incrementally to the commit.
            ctx.dsl().deleteFrom(RUNS).where(RUNS.CHASH.eq(chash)).execute();
            ctx.dsl()
                    .deleteFrom(MEASUREMENTS)
                    .where(MEASUREMENTS.CHASH.eq(chash))
                    .execute();

            return true;
        });

        if (added) {
            // Try to remove previous logs.
            // If this doesn't work, it isn't a big deal:
            // The logs can't be accessed via the API because the corresponding run in the DB no longer exists.
            try {
                repo.deleteRunLogs(chash);
            } catch (IOException ignored) {
            }
        }
    }

    private JsonJob makeJob(Task task, Run run) throws IOException {
        Repo repo = task.repo();
        ServerConfigRepo repoConfig = repo.config();

        String benchChash =
                repo.gitBench().plumbing().resolve(repoConfig.benchRef()).name();

        return new JsonJob(
                repo.name(),
                repoConfig.url(),
                task.chash(),
                repoConfig.benchUrl(),
                benchChash,
                run.name(),
                run.script());
    }

    public Optional<JsonJob> takeJob(String runner) throws IOException {
        for (Task task : getTasks()) {
            for (Run run : task.runs()) {
                if (run.finished().isPresent()) continue;
                if (!run.runner().equals(runner)) continue;
                return Optional.of(makeJob(task, run));
            }
        }
        return Optional.empty();
    }

    public void finishJob(String repoName, String runnerName, JsonRunResult runResult) throws IOException {
        // Intentionally blindly trusting the runner's data.
        // It might be from an older config version.

        Repo repo = repos.repo(repoName);

        repo.db().writeTransaction(ctx -> {
            Set<String> runs = ctx.dsl().select(RUNS.NAME).from(RUNS).where(RUNS.CHASH.eq(runResult.chash())).stream()
                    .map(Record1::value1)
                    .collect(Collectors.toCollection(HashSet::new));

            // Adding run data on top of an existing run with the same name is not a good idea.
            if (runs.contains(runResult.name())) return;

            // Add run data to db
            updateMetrics(ctx, runResult);
            addRun(ctx, runnerName, runResult);
            addMeasurements(ctx, runResult);
            runs.add(runResult.name());

            // Remove task from queue if all its runs are finished
            boolean allRunsFinished = repo.config().benchRuns().stream().allMatch(it -> runs.contains(it.name()));
            if (!allRunsFinished) return;
            ctx.dsl().deleteFrom(QUEUE).where(QUEUE.CHASH.eq(runResult.chash())).execute();
        });

        repo.saveRunLog(runResult.chash(), runResult.name(), runResult.lines());
    }

    private void updateMetrics(Configuration ctx, JsonRunResult runResult) {
        Map<String, MetricsRecord> metrics =
                ctx.dsl().selectFrom(METRICS).stream().collect(Collectors.toMap(MetricsRecord::getMetric, it -> it));

        for (JsonRunResultEntry entry : runResult.entries()) {
            MetricsRecord record = metrics.get(entry.metric());
            if (record == null) {
                MetricsRecord newRecord = new MetricsRecord(
                        entry.metric(),
                        entry.unit().orElse(null),
                        entry.direction().orElse(Constants.DEFAULT_DIRECTION));
                metrics.put(entry.metric(), newRecord);
            } else {
                entry.unit().ifPresent(record::setUnit);
                entry.direction().ifPresent(record::setDirection);
            }
        }

        ctx.dsl().batchStore(metrics.values()).execute();
    }

    private void addRun(Configuration ctx, String runnerName, JsonRunResult runResult) {
        RunsRecord record = new RunsRecord();
        record.setChash(runResult.chash());
        record.setName(runResult.name());
        record.setScript(runResult.script());
        record.setRunner(runnerName);
        record.setChashBench(runResult.benchChash());
        record.setStartTime(runResult.startTime());
        record.setEndTime(runResult.endTime());
        record.setScriptStartTime(runResult.scriptStartTime().orElse(null));
        record.setScriptEndTime(runResult.scriptEndTime().orElse(null));
        record.setExitCode(runResult.exitCode());
        ctx.dsl().batchInsert(record).execute();
    }

    private void addMeasurements(Configuration ctx, JsonRunResult runResult) {
        for (JsonRunResultEntry entry : runResult.entries()) {
            ctx.dsl()
                    .insertInto(MEASUREMENTS, MEASUREMENTS.CHASH, MEASUREMENTS.METRIC, MEASUREMENTS.VALUE)
                    .values(runResult.chash(), entry.metric(), entry.value())
                    .onDuplicateKeyIgnore()
                    .execute();
        }
    }
}
