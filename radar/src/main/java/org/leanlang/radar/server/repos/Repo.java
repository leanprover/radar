package org.leanlang.radar.server.repos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.leanlang.radar.FsUtil;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;

public final class Repo implements AutoCloseable {
    private final ObjectMapper mapper;
    private final Dirs dirs;
    private final ServerConfigRepo config;

    private final RepoDb db;
    private final RepoGit git;
    private final RepoGit gitBench;

    public Repo(ObjectMapper mapper, Dirs dirs, ServerConfigRepo config) throws IOException {
        this.mapper = mapper;
        this.dirs = dirs;
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

    public void saveRunLog(String chash, String run, List<JsonOutputLine> lines) throws IOException {
        Path file = dirs.repoRunLog(config.name(), chash, run);
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            for (JsonOutputLine line : lines) {
                writer.write(mapper.writeValueAsString(line));
                writer.newLine();
            }
        }
    }

    public List<JsonOutputLine> loadRunLog(String chash, String run) throws IOException {
        Path file = dirs.repoRunLog(config.name(), chash, run);
        List<JsonOutputLine> lines = new ArrayList<>();
        for (String line : Files.readString(file).lines().toList()) {
            lines.add(mapper.readValue(line, JsonOutputLine.class));
        }
        return lines;
    }

    public void deleteRunLogs(String chash) throws IOException {
        Path dir = dirs.repoRunLogs(config.name(), chash);
        FsUtil.removeDirRecursively(dir);
    }
}
