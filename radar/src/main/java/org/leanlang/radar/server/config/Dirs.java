package org.leanlang.radar.server.config;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

public final class Dirs {
    private final Path state;
    private final Path cache;

    public Dirs(Path configFile, @Nullable Path stateDir, @Nullable Path cacheDir, ServerConfigDirs dirs) {
        Path root = configFile.getParent();
        state = stateDir != null ? stateDir : root.resolve(dirs.state);
        cache = cacheDir != null ? cacheDir : root.resolve(dirs.cache);
    }

    private Path repoState(String repo) {
        return state.resolve("repos").resolve(repo);
    }

    private Path repoCache(String repo) {
        return cache.resolve("repos").resolve(repo);
    }

    public Path repoDb(String repo) {
        return repoState(repo).resolve("data.db");
    }

    public Path repoRunLogs(String repo, String chash) {
        return repoState(repo).resolve("logs").resolve(chash);
    }

    public Path repoRunLog(String repo, String chash, String run) {
        return repoRunLogs(repo, chash).resolve(run + ".jsonl");
    }

    public Path repoGit(String repo) {
        return repoCache(repo).resolve("repo.git");
    }

    public Path repoGitBench(String repo) {
        return repoCache(repo).resolve("bench.git");
    }
}
