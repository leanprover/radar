package org.leanlang.radar.server.config;

import java.nio.file.Path;

public final class Dirs {
    private final Path state;
    private final Path cache;

    public Dirs(Path configFile, ServerConfigDirs dirs) {
        Path root = configFile.getParent();
        state = root.resolve(dirs.state());
        cache = root.resolve(dirs.cache());
    }

    public Path repoDb(String repo) {
        return state.resolve(repo).resolve("data.db");
    }

    public Path repoGit(String repo) {
        return cache.resolve(repo).resolve("repo.git");
    }

    public Path repoGitBench(String repo) {
        return cache.resolve(repo).resolve("bench.git");
    }
}
