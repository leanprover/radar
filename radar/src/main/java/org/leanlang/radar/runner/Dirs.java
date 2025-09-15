package org.leanlang.radar.runner;

import java.nio.file.Path;

public final class Dirs {
    private final Path cache;
    private final Path tmp;

    public Dirs(Path configFile, RunnerConfigDirs dirs) {
        Path root = configFile.getParent();
        cache = root.resolve(dirs.cache());
        tmp = root.resolve(dirs.tmp());
    }

    public Path bareRepo(String repo) {
        return cache.resolve("repos").resolve(repo + ".git");
    }

    public Path bareBenchRepo(String repo) {
        return cache.resolve("benchrepos").resolve(repo + ".git");
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
