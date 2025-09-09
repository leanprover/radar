package org.leanlang.radar.server.config;

import java.nio.file.Path;

public class Dirs {
    private final Path data;
    private final Path cache;
    private final Path tmp;

    public Dirs(final Path configFile, final ServerConfigDirs dirs) {
        final Path root = configFile.getParent();
        data = root.resolve(dirs.data());
        cache = root.resolve(dirs.cache());
        tmp = root.resolve(dirs.tmp());
    }

    public Path repoDb(final String name) {
        return data.resolve(name).resolve("data.db");
    }
}
