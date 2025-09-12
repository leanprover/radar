package org.leanlang.radar.server.queue;

import static org.leanlang.radar.codegen.jooq.Tables.QUEUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.leanlang.radar.codegen.jooq.tables.records.QueueRecord;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.runners.Runners;

public final class Queue {
    private final Repos repos;
    private final Runners runners;
    private final List<Task> activeTasks;

    public Queue(Repos repos, Runners runners) {
        this.repos = repos;
        this.runners = runners;
        this.activeTasks = new ArrayList<>();
    }

    public synchronized List<Task> getAllTasks() throws IOException {
        List<Task> result = new ArrayList<>();
        Set<TaskId> seen = new HashSet<>();

        for (Task task : activeTasks) {
            assert !seen.contains(task.id());
            result.add(task);
            seen.add(task.id());
        }

        for (Task task : getQueueTasks()) {
            if (seen.contains(task.id())) continue;
            result.add(task);
            seen.add(task.id());
        }

        return result;
    }

    private synchronized List<Task> getQueueTasks() throws IOException {
        List<Task> result = new ArrayList<>();

        for (Repo repo : repos.repos()) {
            String benchChash =
                    repo.gitBench().plumbing().resolve(repo.config().benchRef()).name();

            for (QueueRecord entry : repo.db().read().dsl().selectFrom(QUEUE).fetch()) {
                List<Run> runs = repo.config().benchRuns().stream()
                        .map(it -> new Run(runners.runner(it.runner()), it.script()))
                        .toList();
                result.add(new Task(
                        repo, entry.getChash(), benchChash, runs, entry.getQueuedTime(), entry.getBumpedTime()));
            }
        }

        result.sort(Comparator.comparing(Task::bumped).reversed());
        return result;
    }
}
