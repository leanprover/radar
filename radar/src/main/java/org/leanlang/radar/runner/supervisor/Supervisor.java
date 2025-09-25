package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.Constants;
import org.leanlang.radar.runner.StatusUpdater;
import org.leanlang.radar.runner.config.Dirs;
import org.leanlang.radar.runner.config.RunnerConfig;
import org.leanlang.radar.server.api.ResQueueRunnerFinish;
import org.leanlang.radar.server.api.ResQueueRunnerTake;
import org.leanlang.radar.server.data.RepoGit;
import org.leanlang.radar.server.queue.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Supervisor {
    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final RunnerConfig config;
    private final Dirs dirs;
    private final Client client;
    private final ObjectMapper mapper;

    private @Nullable SupervisorStatus status;

    public Supervisor(RunnerConfig config, Dirs dirs, Client client, ObjectMapper mapper) {
        this.config = config;
        this.dirs = dirs;
        this.client = client;
        this.mapper = mapper;
    }

    public synchronized Optional<SupervisorStatus> status() {
        return Optional.ofNullable(status);
    }

    private synchronized void setStatus(@Nullable SupervisorStatus status) {
        this.status = status;
    }

    /**
     * @return true if a run was performed, which indicates that this method should immediately be called again.
     */
    public boolean run() throws InterruptedException {
        log.info("Acquiring job");
        Optional<Job> jobOpt = acquireRun();
        if (jobOpt.isEmpty()) return false;
        Job job = jobOpt.get();
        log.debug("Acquired job {}", job);

        OutputLines lines = new OutputLines();
        setStatus(new SupervisorStatus(job, lines));

        Instant startTime = Instant.now();
        Instant scriptStartTime = null;
        Instant scriptEndTime = null;
        int exitCode;
        List<JsonRunResultEntry> entries = new ArrayList<>();
        try {
            clearTmpDir();
            fetchAndCloneRepo(job);
            fetchAndCloneBenchRepo(job);
            try (BenchScriptExecutor benchScriptExecutor = new BenchScriptExecutor(dirs, job, lines)) {
                scriptStartTime = Instant.now();
                exitCode = benchScriptExecutor.result();
                scriptEndTime = Instant.now();
            }
            entries = readResultFile();
            clearTmpDir(); // Being considerate towards the next run :)
        } catch (Exception e) {
            lines.add(e);
            exitCode = -1;
            if (scriptStartTime != null) scriptEndTime = Instant.now();
        } finally {
            setStatus(null);
        }
        Instant endTime = Instant.now();

        JsonRunResult result = new JsonRunResult(
                job, startTime, endTime, scriptStartTime, scriptEndTime, exitCode, entries, lines.getAll());

        while (true) {
            try {
                submitResult(result);
                break;
            } catch (Exception e) {
                log.error("Failed to submit result, retrying soon...", e);
                Thread.sleep(Constants.RUNNER_SUBMIT_RESULT_DELAY);
            }
        }

        return true;
    }

    private Optional<Job> acquireRun() {
        return client.target(config.apiUrl(ResQueueRunnerTake.PATH))
                .request(MediaType.APPLICATION_JSON)
                .post(
                        Entity.json(new ResQueueRunnerTake.JsonPostInput(config.name, config.token)),
                        ResQueueRunnerTake.JsonPost.class)
                .job()
                .map(ResQueueRunnerTake.JsonJob::toJob);
    }

    private void clearTmpDir() throws IOException {
        log.debug("Clearing tmp directory");

        Path dir = dirs.tmp();
        if (Files.notExists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void fetchAndCloneRepo(Job job) throws Exception {
        try (RepoGit repo = new RepoGit(dirs.bareRepo(job.repo()), job.url())) {
            log.debug("Fetching repo");
            repo.fetch();
            log.debug("Cloning repo");
            repo.cloneTo(dirs.tmpRepo(), job.chash());
        }
    }

    private void fetchAndCloneBenchRepo(Job job) throws Exception {
        try (RepoGit repo = new RepoGit(dirs.bareBenchRepo(job.repo()), job.benchUrl())) {
            log.debug("Fetching bench repo");
            repo.fetch();
            log.debug("Cloning bench repo");
            repo.cloneTo(dirs.tmpBenchRepo(), job.benchChash());
        }
    }

    private List<JsonRunResultEntry> readResultFile() throws Exception {
        List<JsonRunResultEntry> entries = new ArrayList<>();
        for (String line : Files.readString(dirs.tmpResultFile()).lines().toList()) {
            entries.add(mapper.readValue(line, JsonRunResultEntry.class));
        }
        return entries;
    }

    private void submitResult(JsonRunResult runResult) {
        log.debug("Submitting result");

        // We need to prevent the situation where we send a status update telling the server we're still working on
        // our run after we've already submitted the results. Otherwise, the run will land on the queue again.
        new StatusUpdater(config, this, client).runAndThrow();

        Response response = client.target(config.apiUrl(ResQueueRunnerFinish.PATH))
                .request()
                .post(Entity.json(new ResQueueRunnerFinish.JsonPostInput(config.name, config.token, runResult)));

        if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
            throw new WebApplicationException(response);
        }

        log.debug("Submitted result");
    }
}
