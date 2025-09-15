package org.leanlang.radar.server.runners;

import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.config.ServerConfigRunner;

public final class Runner {
    private final ServerConfigRunner config;
    private @Nullable RunnerStatus status;

    public Runner(ServerConfigRunner config) {
        this.config = config;
    }

    public String name() {
        return config.name();
    }

    public ServerConfigRunner config() {
        return config;
    }

    public synchronized Optional<RunnerStatus> status() {
        return Optional.ofNullable(status);
    }

    public synchronized void setStatus(RunnerStatus status) {
        this.status = status;
    }
}
