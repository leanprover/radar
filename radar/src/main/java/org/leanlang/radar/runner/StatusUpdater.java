package org.leanlang.radar.runner;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.leanlang.radar.runner.supervisor.Supervisor;
import org.leanlang.radar.server.api.ResQueueRunnerStatus;

public record StatusUpdater(RunnerConfig config, Supervisor supervisor, Client client) {
    public void run() {
        Optional<ResQueueRunnerStatus.JsonRunId> activeRun =
                supervisor.status().map(ResQueueRunnerStatus.JsonRunId::fromRunId);

        client.target(config.apiUrl(ResQueueRunnerStatus.PATH))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new ResQueueRunnerStatus.JsonPostInput(config.name(), config.token(), activeRun)));
    }
}
