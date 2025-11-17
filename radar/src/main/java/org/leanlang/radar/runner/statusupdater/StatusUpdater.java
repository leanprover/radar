package org.leanlang.radar.runner.statusupdater;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.leanlang.radar.Constants;
import org.leanlang.radar.runner.config.RunnerConfig;
import org.leanlang.radar.runner.supervisor.JsonOutputLineBatch;
import org.leanlang.radar.runner.supervisor.Supervisor;
import org.leanlang.radar.server.api.ResQueueRunnerStatus;
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
        Optional<JsonActiveRun> activeRun = supervisor.status().map(it -> {
            JsonOutputLineBatch lines = it.lines().getLast(Constants.RUNNER_STATUS_UPDATE_LINES);
            return new JsonActiveRun(it.job(), it.startTime(), lines);
        });

        client.target(config.apiUrl(ResQueueRunnerStatus.PATH))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new ResQueueRunnerStatus.JsonPostInput(config.name, config.token, activeRun)))
                .close();
    }
}
