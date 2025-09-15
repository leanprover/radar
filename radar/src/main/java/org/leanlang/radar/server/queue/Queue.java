package org.leanlang.radar.server.queue;

import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.MetricsRecord;
import org.leanlang.radar.codegen.jooq.tables.records.QueueRecord;
import org.leanlang.radar.codegen.jooq.tables.records.RunsRecord;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;

public final class Queue {
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
                        .map(it -> new Run(it.runner(), it.script()))
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

    private synchronized void removeActiveTaskIfExists(ActiveTask activeTask) {
        activeTasks.remove(activeTask);
    }

    private Job jobFromActiveTask(ActiveTask task, String script) {
        return new Job(
                task.repo().name(),
                task.repo().config().url(),
                task.chash(),
                task.repo().config().benchUrl(),
                task.benchChash(),
                script);
    }

    public Optional<Job> takeJob(String runner) throws IOException {
        // Look in active tasks, FIFO
        for (ActiveTask activeTask : getActiveTasks()) {
            for (Run run : activeTask.uncompletedRuns()) {
                if (!run.runner().equals(runner)) continue;
                return Optional.of(jobFromActiveTask(activeTask, run.script()));
            }
        }

        // Look in remaining queue
        for (Task task : getQueuedTasks()) {
            for (Run run : task.runs()) {
                if (!run.runner().equals(runner)) continue;
                TaskId id = new TaskId(task.repo(), task.chash());
                ActiveTask activeTask = ensureActiveTaskExists(id);
                return Optional.of(jobFromActiveTask(activeTask, run.script()));
            }
        }

        return Optional.empty();
    }

    public synchronized void finishJob(String repo, RunResult runResult) throws IOException {
        ActiveTask task = ensureActiveTaskExists(new TaskId(repo, runResult.chash()));
        task.addResult(runResult);
        if (!task.uncompletedRuns().isEmpty()) return;

        task.repo().db().writeTransaction(ctx -> {
            List<RunsRecord> runs = task.results().stream()
                    .map(it -> {
                        RunsRecord record = new RunsRecord();
                        record.setChash(it.chash());
                        record.setRunner(it.runner());
                        record.setScript(it.script());
                        record.setChashBench(it.benchChash());
                        record.setStartTime(it.startTime());
                        record.setEndTime(it.endTime());
                        return record;
                    })
                    .toList();

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

            List<MetricsRecord> metrics = task.results().stream()
                    .flatMap(it -> it.entries().stream())
                    .map(it -> {
                        MetricsRecord record = new MetricsRecord();
                        record.setMetric(it.metric());
                        record.setUnit(it.unit().orElse(null));
                        record.setDirection(it.direction());
                        return record;
                    })
                    .toList();

            ctx.dsl().deleteFrom(RUNS).where(RUNS.CHASH.eq(task.chash())).execute();

            ctx.dsl()
                    .deleteFrom(RUNS)
                    .where(MEASUREMENTS.CHASH.eq(task.chash()))
                    .execute();

            ctx.dsl().batchStore(metrics).execute();

            ctx.dsl().batchInsert(runs).execute();
            ctx.dsl().batchInsert(measurements).execute();
        });

        activeTasks.remove(task);
    }
}
