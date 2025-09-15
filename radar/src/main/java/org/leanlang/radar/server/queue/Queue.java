package org.leanlang.radar.server.queue;

import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.leanlang.radar.codegen.jooq.tables.records.QueueRecord;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.runners.Runners;

public final class Queue {
    private final Repos repos;
    private final Runners runners;
    private final List<ActiveTask> activeTasks;

    public Queue(Repos repos, Runners runners) {
        this.repos = repos;
        this.runners = runners;
        this.activeTasks = new ArrayList<>();
    }

    public synchronized List<ActiveTask> getActiveTasks() {
        return activeTasks.stream().toList();
    }

    public synchronized List<Task> getQueuedTasks() {
        Set<TaskId> activeTaskIds = activeTasks.stream().map(ActiveTask::id).collect(Collectors.toUnmodifiableSet());

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

    public synchronized void ensureActiveTaskExists(TaskId id) throws IOException {
        if (activeTasks.stream().anyMatch(it -> it.id().equals(id))) return;
        ActiveTask task = new ActiveTask(repos.repo(id.repo()), id.chash());
        activeTasks.add(task);
    }
}
