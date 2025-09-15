package org.leanlang.radar.runner.supervisor;

import jakarta.ws.rs.client.Client;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.runner.RunnerConfig;
import org.leanlang.radar.server.queue.RunId;
import org.leanlang.radar.server.queue.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Supervisor {
    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final RunnerConfig config;
    private final Client client;

    private @Nullable RunId activeRun;

    public Supervisor(RunnerConfig config, Client client) {
        this.config = config;
        this.client = client;
    }

    public synchronized Optional<RunId> status() {
        return Optional.ofNullable(activeRun);
    }

    private synchronized void setStatus(@Nullable RunId activeRun) {
        this.activeRun = activeRun;
    }

    /**
     * @return true if a run was performed, which indicates that this method should immediately be called again.
     */
    public boolean run() {
        log.info("Acquiring run");
        Optional<RunId> runOpt = acquireRun();
        if (runOpt.isEmpty()) return false;
        RunId run = runOpt.get();

        setStatus(run);
        try {
            clearTmpDir();
            Path repo = fetchAndCloneRepo(run);
            Path benchRepo = fetchAndCloneBenchRepo(run);
            RunResult result = runBenchScript(benchRepo, repo, run.script());
            submitResult(result);
            return true;
        } finally {
            setStatus(null);
        }
    }

    private Optional<RunId> acquireRun() {
        throw new NotImplementedException(); // TODO
    }

    private void clearTmpDir() {
        throw new NotImplementedException(); // TODO
    }

    private Path fetchAndCloneRepo(RunId run) {
        throw new NotImplementedException(); // TODO
    }

    private Path fetchAndCloneBenchRepo(RunId run) {
        throw new NotImplementedException(); // TODO
    }

    private RunResult runBenchScript(Path benchRepo, Path repo, String script) {
        throw new NotImplementedException(); // TODO
    }

    private void submitResult(RunResult runResult) {
        throw new NotImplementedException(); // TODO
    }
}
