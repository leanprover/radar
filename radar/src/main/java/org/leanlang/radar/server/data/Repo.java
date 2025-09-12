package org.leanlang.radar.server.data;

import java.io.Closeable;
import java.io.IOException;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;

public final class Repo implements Closeable {

    private final ServerConfigRepo config;
    private final RepoDb db;
    private final RepoGit git;
    private final RepoGit gitBench;

    public Repo(Dirs dirs, ServerConfigRepo config) throws IOException {
        this.config = config;
        this.db = new RepoDb(config.name(), dirs.repoDb(config.name()));
        this.git = new RepoGit(dirs.repoGit(config.name()), config.url());
        this.gitBench = new RepoGit(dirs.repoGitBench(config.name()), config.benchUrl());
    }

    @Override
    public void close() {
        db.close();
        git.close();
        gitBench.close();
    }

    public String name() {
        return config.name();
    }

    public ServerConfigRepo config() {
        return config;
    }

    public RepoDb db() {
        return db;
    }

    public RepoGit git() {
        return git;
    }

    public RepoGit gitBench() {
        return gitBench;
    }
}
