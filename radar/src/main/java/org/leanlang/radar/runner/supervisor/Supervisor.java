package org.leanlang.radar.runner.supervisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.Constants;
import org.leanlang.radar.FsUtil;
import org.leanlang.radar.runner.config.Dirs;
import org.leanlang.radar.runner.config.RunnerConfig;
import org.leanlang.radar.runner.statusupdater.StatusUpdater;
import org.leanlang.radar.server.api.ResQueueRunnerFinish;
import org.leanlang.radar.server.api.ResQueueRunnerTake;
import org.leanlang.radar.server.repos.RepoGit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Supervisor {
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
        Optional<JsonJob> jobOpt = acquireRun();
        if (jobOpt.isEmpty()) return false;
        JsonJob job = jobOpt.get();
        log.debug("Acquired job {}", job);

        Instant startTime = Instant.now();
        OutputLines lines = new OutputLines();
        setStatus(new SupervisorStatus(job, startTime, lines));

        Instant scriptStartTime = null;
        Instant scriptEndTime = null;
        int exitCode;
        List<JsonRunResultEntry> entries = new ArrayList<>();
        try {
            lines.addInternal("Clearing tmp directory...");
            FsUtil.removeDirRecursively(dirs.tmp());

            fetchAndCloneRepo(lines, job);
            fetchAndCloneBenchRepo(lines, job);

            lines.addInternal("Executing bench script...");
            try (BenchScriptExecutor benchScriptExecutor = new BenchScriptExecutor(dirs, job, lines)) {
                scriptStartTime = Instant.now();
                exitCode = benchScriptExecutor.result();
                scriptEndTime = Instant.now();
            }

            entries.addAll(readResultFile(lines));

            // Being considerate towards the next run :)
            lines.addInternal("Clearing tmp directory again...");
            FsUtil.removeDirRecursively(dirs.tmp());

            lines.addInternal("Run complete, yay :D");
        } catch (Exception e) {
            lines.addInternal(e);
            exitCode = -1;
            if (scriptStartTime != null) scriptEndTime = Instant.now();
        } finally {
            setStatus(null);
        }

        Instant endTime = Instant.now();

        entries.add(new JsonRunResultEntry(
                "radar/run/" + job.name() + "//time",
                (float) (Duration.between(startTime, endTime).toMillis() / 1000.0),
                Optional.of("s")));

        if (scriptStartTime != null && scriptEndTime != null)
            entries.add(new JsonRunResultEntry(
                    "radar/run/" + job.name() + "/script//time",
                    (float) (Duration.between(scriptStartTime, scriptEndTime).toMillis() / 1000.0),
                    Optional.of("s")));

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

    private Optional<JsonJob> acquireRun() {
        return client.target(config.apiUrl(ResQueueRunnerTake.PATH))
                .request(MediaType.APPLICATION_JSON)
                .post(
                        Entity.json(new ResQueueRunnerTake.JsonPostInput(config.name, config.token)),
                        ResQueueRunnerTake.JsonPost.class)
                .job();
    }

    private void fetchAndCloneRepo(OutputLines lines, JsonJob job) throws Exception {
        try (RepoGit repo = new RepoGit(dirs.bareRepo(job.repo()), job.url())) {
            lines.addInternal("Fetching repo...");
            repo.fetch();
            lines.addInternal("Cloning repo...");
            repo.cloneTo(dirs.tmpRepo(), job.chash());
        }
    }

    private void fetchAndCloneBenchRepo(OutputLines lines, JsonJob job) throws Exception {
        try (RepoGit repo = new RepoGit(dirs.bareBenchRepo(job.repo()), job.benchUrl())) {
            lines.addInternal("Fetching bench repo...");
            repo.fetch();
            lines.addInternal("Cloning bench repo...");
            repo.cloneTo(dirs.tmpBenchRepo(), job.benchChash());
        }
    }

    private List<JsonRunResultEntry> readResultFile(OutputLines lines) throws Exception {
        lines.addInternal("Reading result file...");
        List<JsonRunResultEntry> entries = new ArrayList<>();

        String contents;
        try {
            contents = Files.readString(dirs.tmpResultFile());
        } catch (NoSuchFileException e) {
            lines.addInternal("No result file found.");
            return new ArrayList<>();
        }

        for (String line : contents.lines().toList()) {
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
