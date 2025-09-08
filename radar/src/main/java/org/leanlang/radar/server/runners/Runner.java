package org.leanlang.radar.server.runners;

import org.leanlang.radar.server.config.ServerConfigRunner;

public class Runner {
    private final ServerConfigRunner config;

    public Runner(ServerConfigRunner config) {
        this.config = config;
    }

    public ServerConfigRunner getConfig() {
        return config;
    }
}
