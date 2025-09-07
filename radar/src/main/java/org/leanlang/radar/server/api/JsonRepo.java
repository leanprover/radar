package org.leanlang.radar.server.api;

import org.leanlang.radar.server.RepoConfig;

public record JsonRepo(String name, String url, String description) {
    public static JsonRepo fromRepoConfig(RepoConfig repo) {
        return new JsonRepo(repo.name(), repo.url(), repo.description());
    }
}
