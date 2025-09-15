package org.leanlang.radar.server.queue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.leanlang.radar.server.data.Repo;

public class ActiveTask {
    private final Repo repo;
    private final String chash;
    private final String benchChash;
    private final List<Run> runs;
    private final List<RunResult> results;

    public ActiveTask(Repo repo, String chash) throws IOException {
        this.repo = repo;
        this.chash = chash;

        this.benchChash =
                repo.gitBench().plumbing().resolve(repo.config().benchRef()).name();

        this.runs = repo.config().benchRuns().stream()
                .map(it -> new Run(it.runner(), it.script()))
                .toList();

        this.results = new ArrayList<>();
    }

    public Repo repo() {
        return repo;
    }

    public String chash() {
        return chash;
    }

    public String benchChash() {
        return benchChash;
    }

    public List<Run> runs() {
        return runs;
    }

    public TaskId id() {
        return new TaskId(repo.name(), chash);
    }

    public synchronized List<RunResult> results() {
        return results.stream().toList();
    }

    public synchronized void addResult(RunResult result) {
        results.add(result);
    }
}
