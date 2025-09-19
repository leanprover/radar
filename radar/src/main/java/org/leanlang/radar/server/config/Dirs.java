package org.leanlang.radar.server.config;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

public final class Dirs {
    private final Path state;
    private final Path cache;

    public Dirs(Path configFile, @Nullable Path stateDir, @Nullable Path cacheDir, ServerConfigDirs dirs) {
        Path root = configFile.getParent();
        state = stateDir != null ? stateDir : root.resolve(dirs.state());
        cache = cacheDir != null ? cacheDir : root.resolve(dirs.cache());
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
