package org.leanlang.radar.runner;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.leanlang.radar.runner.supervisor.Supervisor;
import org.leanlang.radar.server.api.ResQueueRunnerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StatusUpdater(RunnerConfig config, Supervisor supervisor, Client client) {
    private static final Logger log = LoggerFactory.getLogger(StatusUpdater.class);

    public void run() {
        try {
            Optional<ResQueueRunnerStatus.JsonRun> activeRun =
                    supervisor.status().map(it -> new ResQueueRunnerStatus.JsonRun(it.repo(), it.chash(), it.script()));

            client.target(config.apiUrl(ResQueueRunnerStatus.PATH))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.json(
                            new ResQueueRunnerStatus.JsonPostInput(config.name(), config.token(), activeRun)));
        } catch (Exception e) {
            log.error("Failed to update status", e);
        }
    }
}
