package org.leanlang.radar.server.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.runner.supervisor.JsonRunResultEntry;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;

public final class Repo implements Closeable {
    private final Dirs dirs;
    private final ServerConfigRepo config;
    private final ObjectMapper mapper;

    private final RepoDb db;
    private final RepoGit git;
    private final RepoGit gitBench;

    public Repo(Dirs dirs, ServerConfigRepo config, ObjectMapper mapper) throws IOException {
        this.dirs = dirs;
        this.config = config;
        this.mapper = mapper;

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

    public void saveRunLog(String chash, List<JsonOutputLine> lines) throws IOException {
        Path file = dirs.repoRunLog(config.name(), chash);
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            for (JsonOutputLine line : lines) {
                writer.write(mapper.writeValueAsString(line));
                writer.newLine();
            }
        }
    }

    public List<JsonOutputLine> loadRunLog(String chash) throws IOException {
        Path file = dirs.repoRunLog(config.name(), chash);
        List<JsonOutputLine> lines = new ArrayList<>();
        for (String line : Files.readString(file).lines().toList()) {
            lines.add(mapper.readValue(line, JsonOutputLine.class));
        }
        return lines;
    }

    public void deleteRunLog(String chash) throws IOException {
        Path file = dirs.repoRunLog(config.name(), chash);
        Files.deleteIfExists(file);
    }
}
