package org.leanlang.radar.server.runners;

import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.config.ServerConfigRunner;
import org.leanlang.radar.server.queue.RunId;

public final class Runner {
    private final ServerConfigRunner config;
    private @Nullable Instant lastSeen;
    private @Nullable RunId activeRun;

    public Runner(ServerConfigRunner config) {
        this.config = config;
    }

    public String name() {
        return config.name();
    }

    public ServerConfigRunner config() {
        return config;
    }

    public synchronized void updateStatus(@Nullable RunId activeRun) {
        this.lastSeen = Instant.now();
        this.activeRun = activeRun;
    }

    public synchronized Optional<Instant> lastSeen() {
        return Optional.ofNullable(lastSeen);
    }

    public synchronized Optional<RunId> activeRun() {
        return Optional.ofNullable(activeRun);
    }
}
