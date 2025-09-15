package org.leanlang.radar.runner.supervisor;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.runner.RunnerConfig;
import org.leanlang.radar.server.api.ResQueueRunnerJobsTake;
import org.leanlang.radar.server.queue.Job;
import org.leanlang.radar.server.queue.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Supervisor {
    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final RunnerConfig config;
    private final Client client;

    private @Nullable Job activeJob;

    public Supervisor(RunnerConfig config, Client client) {
        this.config = config;
        this.client = client;
    }

    public synchronized Optional<Job> status() {
        return Optional.ofNullable(activeJob);
    }

    private synchronized void setStatus(@Nullable Job activeJob) {
        this.activeJob = activeJob;
    }

    /**
     * @return true if a run was performed, which indicates that this method should immediately be called again.
     */
    public boolean run() {
        log.info("Acquiring job");
        Optional<Job> jobOpt = acquireRun();
        if (jobOpt.isEmpty()) return false;
        Job job = jobOpt.get();
        log.debug("Acquired job {}", job);

        setStatus(job);
        try {
            clearTmpDir();
            Path repo = fetchAndCloneRepo(job);
            Path benchRepo = fetchAndCloneBenchRepo(job);
            RunResult result = runBenchScript(benchRepo, repo, job.script());
            submitResult(result);
            return true;
        } finally {
            setStatus(null);
        }
    }

    private Optional<Job> acquireRun() {
        return client.target(config.apiUrl(ResQueueRunnerJobsTake.PATH))
                .request(MediaType.APPLICATION_JSON)
                .post(
                        Entity.json(new ResQueueRunnerJobsTake.JsonPostInput(config.name(), config.token())),
                        ResQueueRunnerJobsTake.JsonPost.class)
                .job()
                .map(ResQueueRunnerJobsTake.JsonJob::toJob);
    }

    private void clearTmpDir() {
        throw new NotImplementedException(); // TODO
    }

    private Path fetchAndCloneRepo(Job job) {
        throw new NotImplementedException(); // TODO
    }

    private Path fetchAndCloneBenchRepo(Job job) {
        throw new NotImplementedException(); // TODO
    }

    private RunResult runBenchScript(Path benchRepo, Path repo, String script) {
        throw new NotImplementedException(); // TODO
    }

    private void submitResult(RunResult runResult) {
        throw new NotImplementedException(); // TODO
    }
}
