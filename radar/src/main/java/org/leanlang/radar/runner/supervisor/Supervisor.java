package org.leanlang.radar.runner.supervisor;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.runner.Dirs;
import org.leanlang.radar.runner.RunnerConfig;
import org.leanlang.radar.server.api.ResQueueRunnerJobsTake;
import org.leanlang.radar.server.data.RepoGit;
import org.leanlang.radar.server.queue.Job;
import org.leanlang.radar.server.queue.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Supervisor {
    private static final Logger log = LoggerFactory.getLogger(Supervisor.class);

    private final RunnerConfig config;
    private final Dirs dirs;
    private final Client client;

    private @Nullable Job activeJob;

    public Supervisor(RunnerConfig config, Dirs dirs, Client client) {
        this.config = config;
        this.dirs = dirs;
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
    public boolean run() throws IOException, GitAPIException {
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
            RunResult result = runBenchScript(job.script());
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

    private void fetchAndCloneRepo(Job job) throws IOException, GitAPIException {
        try (RepoGit repo = new RepoGit(dirs.bareRepo(job.repo()), job.url())) {
            log.debug("Fetching repo");
            repo.fetch();
            log.debug("Cloning repo");
            repo.cloneTo(dirs.tmpRepo(), job.chash());
        }
    }

    private void fetchAndCloneBenchRepo(Job job) throws IOException, GitAPIException {
        try (RepoGit repo = new RepoGit(dirs.bareBenchRepo(job.repo()), job.benchUrl())) {
            log.debug("Fetching bench repo");
            repo.fetch();
            log.debug("Cloning bench repo");
            repo.cloneTo(dirs.tmpBenchRepo(), job.benchChash());
        }
    }

    private RunResult runBenchScript(String script) {
        throw new NotImplementedException(); // TODO
    }

    private void submitResult(RunResult runResult) {
        throw new NotImplementedException(); // TODO
    }
}
