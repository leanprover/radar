package org.leanlang.radar.server.config;

import java.nio.file.Path;

public final class Dirs {
    private final Path data;
    private final Path cache;
    private final Path tmp;

    public Dirs(Path configFile, ServerConfigDirs dirs) {
        Path root = configFile.getParent();
        data = root.resolve(dirs.data());
        cache = root.resolve(dirs.cache());
        tmp = root.resolve(dirs.tmp());
    }

    public Path repoDb(String name) {
        return data.resolve(name).resolve("data.db");
    }

    public Path repoGit(String name) {
        return cache.resolve("repos").resolve(name + ".git");
    }

    public Path repoGitBench(String name) {
        return cache.resolve("benchrepos").resolve(name + ".git");
    }
}
