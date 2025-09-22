package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.FileNotFoundException;
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
import org.leanlang.radar.runner.StatusUpdater;
import org.leanlang.radar.runner.config.Dirs;
import org.leanlang.radar.runner.config.RunnerConfig;
import org.leanlang.radar.server.api.ResQueueRunnerJobsFinish;
import org.leanlang.radar.server.api.ResQueueRunnerJobsTake;
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

    private @Nullable Job activeJob;

    public Supervisor(RunnerConfig config, Dirs dirs, Client client, ObjectMapper mapper) {
        this.config = config;
        this.dirs = dirs;
        this.client = client;
        this.mapper = mapper;
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
    public boolean run() throws Exception {
        log.info("Acquiring job");
        Optional<Job> jobOpt = acquireRun();
        if (jobOpt.isEmpty()) return false;
        Job job = jobOpt.get();
        log.debug("Acquired job {}", job);

        setStatus(job);
        try {
            clearTmpDir();
            fetchAndCloneRepo(job);
            fetchAndCloneBenchRepo(job);
            JsonRunResult result = runBenchScript(job);

            // We need to prevent the situation where we send a status update telling the server we're still working on
            // our run after we've already submitted the results. Otherwise, the run will land on the queue again.
            setStatus(null);
            new StatusUpdater(config, this, client).runAndThrow();

            submitResult(result);
            return true;
        } catch (Exception e) {
            // TODO Include exception trace in log instead of just logging it
            log.debug("Failed to run job {}", job, e);

            Instant now = Instant.now();
            JsonRunResult result = new JsonRunResult(job, now, now, -1);

            // We need to prevent the situation where we send a status update telling the server we're still working on
            // our run after we've already submitted the results. Otherwise, the run will land on the queue again.
            setStatus(null);
            new StatusUpdater(config, this, client).runAndThrow();

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
                        Entity.json(new ResQueueRunnerJobsTake.JsonPostInput(config.name, config.token)),
                        ResQueueRunnerJobsTake.JsonPost.class)
                .job()
                .map(ResQueueRunnerJobsTake.JsonJob::toJob);
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

    private JsonRunResult runBenchScript(Job job) throws Exception {
        // Run the script
        Instant startTime = Instant.now();
        Process process = new ProcessBuilder(
                        dirs.tmpBenchRepoScript(job.script()).toAbsolutePath().toString(),
                        dirs.tmpRepo().toAbsolutePath().toString(),
                        dirs.tmpResultFile().toAbsolutePath().toString())
                .directory(dirs.tmpBenchRepo().toFile())
                .inheritIO()
                .start();
        int exitCode = process.waitFor();
        Instant endTime = Instant.now();

        // Read the resulting data
        List<JsonRunResultEntry> entries = new ArrayList<>();
        try {
            for (String line : Files.readString(dirs.tmpResultFile()).lines().toList()) {
                entries.add(mapper.readValue(line, JsonRunResultEntry.class));
            }
        } catch (FileNotFoundException ignored) {
        }

        // A benchmark has been run :D
        return new JsonRunResult(job, startTime, endTime, exitCode, entries);
    }

    private void submitResult(JsonRunResult runResult) {
        log.debug("Submitting result");
        Response response = client.target(config.apiUrl(ResQueueRunnerJobsFinish.PATH))
                .request()
                .post(Entity.json(new ResQueueRunnerJobsFinish.JsonPostInput(config.name, config.token, runResult)));

        if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
            throw new WebApplicationException(response);
        }
        log.debug("Submitted result");
    }
}
