package org.leanlang.radar.server.api;

import org.leanlang.radar.server.config.ServerConfigRepo;

public record JsonRepo(String name, String url, String description) {
    public static JsonRepo fromConfig(final ServerConfigRepo repo) {
        return new JsonRepo(repo.name(), repo.url().toString(), repo.description());
    }
}
