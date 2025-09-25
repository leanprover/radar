package org.leanlang.radar.server.queue;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.leanlang.radar.Constants;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.MetricsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.QueueRecord;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Queue {
    private static final Logger log = LoggerFactory.getLogger(Queue.class);
    private final Repos repos;
    private final List<ActiveTask> activeTasks;

    public Queue(Repos repos) {
        this.repos = repos;
        this.activeTasks = new ArrayList<>();
    }

    public synchronized List<ActiveTask> getActiveTasks() {
        return activeTasks.stream().toList();
    }

    public List<Task> getQueuedTasks() {
        Set<TaskId> activeTaskIds =
                getActiveTasks().stream().map(ActiveTask::id).collect(Collectors.toUnmodifiableSet());

        List<Task> result = new ArrayList<>();
        for (Repo repo : repos.repos()) {
            for (QueueRecord entry : repo.db().read().dsl().selectFrom(QUEUE).fetch()) {
                TaskId id = new TaskId(repo.name(), entry.getChash());
                if (activeTaskIds.contains(id)) continue;

                List<Run> runs = repo.config().benchRuns().stream()
                        .map(it -> new Run(it.name(), it.script(), it.runner()))
                        .toList();
                Task task = new Task(id.repo(), id.chash(), runs, entry.getQueuedTime(), entry.getBumpedTime());
                result.add(task);
            }
        }

        result.sort(Comparator.comparing(Task::bumped).reversed());
        return result;
    }

    public synchronized ActiveTask ensureActiveTaskExists(TaskId id) throws IOException {
        Optional<ActiveTask> existingTask =
                activeTasks.stream().filter(it -> it.id().equals(id)).findFirst();
        if (existingTask.isPresent()) return existingTask.get();

        ActiveTask newTask = new ActiveTask(repos.repo(id.repo()), id.chash());
        activeTasks.add(newTask);
        return newTask;
    }

    private Job jobFromActiveTask(ActiveTask task, Run run) {
        return new Job(
                task.repo().name(),
                task.repo().config().url(),
                task.chash(),
                task.repo().config().benchUrl(),
                task.benchChash(),
                run.name(),
                run.script());
    }

    public Optional<Job> takeJob(String runner) throws IOException {
        // Look in active tasks, FIFO
        for (ActiveTask activeTask : getActiveTasks()) {
            for (Run run : activeTask.uncompletedRuns()) {
                if (!run.runner().equals(runner)) continue;
                return Optional.of(jobFromActiveTask(activeTask, run));
            }
        }

        // Look in remaining queue
        for (Task task : getQueuedTasks()) {
            for (Run run : task.runs()) {
                if (!run.runner().equals(runner)) continue;
                TaskId id = new TaskId(task.repo(), task.chash());
                ActiveTask activeTask = ensureActiveTaskExists(id);
                return Optional.of(jobFromActiveTask(activeTask, run));
            }
        }

        return Optional.empty();
    }

    private void updateMetrics(Configuration ctx, ActiveTask task) {
        Map<String, MetricsRecord> metrics =
                ctx.dsl().selectFrom(METRICS).stream().collect(Collectors.toMap(MetricsRecord::getMetric, it -> it));

        task.results().stream().flatMap(it -> it.entries().stream()).forEach(entry -> {
            MetricsRecord record = metrics.get(entry.metric());
            if (record == null) {
                metrics.put(
                        entry.metric(),
                        new MetricsRecord(
                                entry.metric(),
                                entry.unit().orElse(null),
                                entry.direction().orElse(Constants.DEFAULT_DIRECTION)));
            } else {
                entry.unit().ifPresent(record::setUnit);
                entry.direction().ifPresent(record::setDirection);
            }
        });

        ctx.dsl().batchStore(metrics.values()).execute();
    }

    private void updateRuns(Configuration ctx, ActiveTask task) {
        List<RunsRecord> runs = task.results().stream()
                .map(it -> {
                    RunsRecord record = new RunsRecord();
                    record.setChash(it.chash());
                    record.setName(it.run().name());
                    record.setScript(it.run().script());
                    record.setRunner(it.run().runner());
                    record.setChashBench(it.benchChash());
                    record.setStartTime(it.startTime());
                    record.setEndTime(it.endTime());
                    record.setExitCode(it.exitCode());
                    return record;
                })
                .toList();

        ctx.dsl().deleteFrom(RUNS).where(RUNS.CHASH.eq(task.chash())).execute();
        ctx.dsl().batchInsert(runs).execute();
    }

    private void updateMeasurements(Configuration ctx, ActiveTask task) {
        List<MeasurementsRecord> measurements = task.results().stream()
                .flatMap(it -> it.entries().stream())
                .map(it -> {
                    MeasurementsRecord record = new MeasurementsRecord();
                    record.setChash(task.chash());
                    record.setMetric(it.metric());
                    record.setValue(it.value());
                    return record;
                })
                .toList();

        ctx.dsl()
                .deleteFrom(MEASUREMENTS)
                .where(MEASUREMENTS.CHASH.eq(task.chash()))
                .execute();

        ctx.dsl().batchInsert(measurements).execute();
    }

    public synchronized void finishJob(String repo, RunResult runResult) throws IOException {
        ActiveTask task = ensureActiveTaskExists(new TaskId(repo, runResult.chash()));
        task.addResult(runResult);
        if (!task.uncompletedRuns().isEmpty()) return;

        task.repo().deleteRunLog(task.chash());

        task.repo().db().writeTransaction(ctx -> {
            log.debug("Updating metrics");
            updateMetrics(ctx, task);

            log.debug("Updating runs");
            updateRuns(ctx, task);

            log.debug("Updating measurements");
            updateMeasurements(ctx, task);

            log.debug("Removing from queue");
            ctx.dsl().deleteFrom(QUEUE).where(QUEUE.CHASH.eq(task.chash())).execute();

            log.debug("Done");
        });

        task.repo().saveRunLog(task.chash(), runResult.lines());

        activeTasks.remove(task);
    }
}
