package org.leanlang.radar.server.data;

import java.io.Closeable;
import java.io.IOException;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;

public final class Repo implements Closeable {

    private final ServerConfigRepo config;
    private final RepoDb db;
    private final RepoGit git;

    public Repo(Dirs dirs, ServerConfigRepo config) throws IOException {
        this.config = config;
        this.db = new RepoDb(config.name(), dirs.repoDb(config.name()));
        this.git = new RepoGit(config.name(), dirs.repoGit(config.name()), config.url());
    }

    @Override
    public void close() {
        db.close();
        git.close();
    }

    public ServerConfigRepo getConfig() {
        return config;
    }

    public RepoDb getDb() {
        return db;
    }

    public RepoGit getGit() {
        return git;
    }
}
