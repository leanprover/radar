package org.leanlang.radar.runner;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.leanlang.radar.Constants;
import org.leanlang.radar.runner.config.RunnerConfig;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.runner.supervisor.Supervisor;
import org.leanlang.radar.server.api.ResQueueRunnerStatus;
import org.leanlang.radar.server.api.ResQueueRunnerTake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StatusUpdater(RunnerConfig config, Supervisor supervisor, Client client) {
    private static final Logger log = LoggerFactory.getLogger(StatusUpdater.class);

    public void run() {
        try {
            runAndThrow();
        } catch (Exception e) {
            log.error("Failed to update status", e);
        }
    }

    public void runAndThrow() {
        Optional<ResQueueRunnerStatus.JsonRun> activeRun = supervisor
                .status()
                .map(it -> new ResQueueRunnerStatus.JsonRun(
                        new ResQueueRunnerTake.JsonJob(it.job()),
                        it.startTime(),
                        it.lines().getLast(Constants.RUNNER_STATUS_UPDATE_LINES).stream()
                                .map(JsonOutputLine::new)
                                .toList()));

        client.target(config.apiUrl(ResQueueRunnerStatus.PATH))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new ResQueueRunnerStatus.JsonPostInput(config.name, config.token, activeRun)));
    }
}
