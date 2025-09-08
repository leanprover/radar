package org.leanlang.radar.server.api;

import org.leanlang.radar.server.config.RadarConfigRepo;

public record JsonRepo(String name, String url, String description) {
    public static JsonRepo fromConfig(final RadarConfigRepo repo) {
        return new JsonRepo(repo.name(), repo.url().toString(), repo.description());
    }
}
