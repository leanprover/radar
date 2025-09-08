package org.leanlang.radar.server.runners;

import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.config.ServerConfigRunner;

public class Runner {
    private final ServerConfigRunner config;
    private @Nullable Instant lastSeen;

    public Runner(ServerConfigRunner config) {
        this.config = config;
    }

    public ServerConfigRunner getConfig() {
        return config;
    }

    public synchronized Instant see() {
        lastSeen = Instant.now();
        return lastSeen;
    }

    public synchronized Optional<Instant> lastSeen() {
        return Optional.ofNullable(lastSeen);
    }
}
