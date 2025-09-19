package org.leanlang.radar.runner.config;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

public final class Dirs {
    private final Path cache;
    private final Path tmp;

    public Dirs(Path configFile, @Nullable Path cacheDir, @Nullable Path tmpDir, RunnerConfigDirs dirs) {
        Path root = configFile.getParent();
        cache = cacheDir != null ? cacheDir : root.resolve(dirs.cache);
        tmp = tmpDir != null ? tmpDir : root.resolve(dirs.tmp);
    }

    public Path bareRepo(String repo) {
        return cache.resolve(repo).resolve("repo.git");
    }

    public Path bareBenchRepo(String repo) {
        return cache.resolve(repo).resolve("bench.git");
    }

    public Path tmp() {
        return tmp;
    }

    public Path tmpRepo() {
        return tmp.resolve("repo");
    }

    public Path tmpBenchRepo() {
        return tmp.resolve("bench");
    }

    public Path tmpBenchRepoScript(String script) {
        return tmp.resolve("bench").resolve(script);
    }

    public Path tmpResultFile() {
        return tmp.resolve("result.jsonl");
    }
}
