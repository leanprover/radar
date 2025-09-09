package org.leanlang.radar.server.data;

import java.io.Closeable;
import java.io.IOException;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;

public class Repo implements Closeable {

    private final ServerConfigRepo config;
    private final RepoDb db;

    public Repo(final Dirs dirs, final ServerConfigRepo config) throws IOException {
        this.config = config;
        this.db = new RepoDb(dirs.repoDb(config.name()), config.name());
    }

    @Override
    public void close() {
        db.close();
    }

    public ServerConfigRepo getConfig() {
        return config;
    }

    public RepoDb getDb() {
        return db;
    }
}
