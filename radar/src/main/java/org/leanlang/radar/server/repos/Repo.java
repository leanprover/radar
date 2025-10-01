package org.leanlang.radar.server.repos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.FsUtil;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;
import org.leanlang.radar.server.config.ServerConfigRepoRun;
import org.leanlang.radar.server.repos.source.RepoSource;

public final class Repo implements AutoCloseable {
    private final ObjectMapper mapper;
    private final Dirs dirs;

    private final String name;
    private final String description;
    private final RepoSource source;
    private final Set<String> track;
    private final RepoSource benchSource;
    private final String benchRef;
    private final List<ServerConfigRepoRun> benchRuns;

    private final RepoDb db;
    private final RepoGit git;
    private final RepoGit gitBench;

    public Repo(ObjectMapper mapper, Dirs dirs, ServerConfigRepo config, @Nullable Path githubPatFile)
            throws IOException {

        this.mapper = mapper;
        this.dirs = dirs;

        this.name = config.name();
        this.description = config.description();
        this.source = RepoSource.parse(config.url());
        this.track = config.track().stream().collect(Collectors.toUnmodifiableSet()); // Just to make sure
        this.benchSource = RepoSource.parse(config.benchUrl());
        this.benchRef = config.benchRef();
        this.benchRuns = config.benchRuns();

        this.db = new RepoDb(this.name, dirs.repoDb(this.name));
        this.git = new RepoGit(dirs.repoGit(this.name), this.source.gitUrl());
        this.gitBench = new RepoGit(dirs.repoGitBench(this.name), this.benchSource.gitUrl());
    }

    @Override
    public void close() {
        db.close();
        git.close();
        gitBench.close();
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public RepoSource source() {
        return source;
    }

    public Set<String> track() {
        return track;
    }

    public RepoSource benchSource() {
        return benchSource;
    }

    public String benchRef() {
        return benchRef;
    }

    public List<ServerConfigRepoRun> benchRuns() {
        return benchRuns;
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
        Path file = dirs.repoRunLog(this.name, chash, run);
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            for (JsonOutputLine line : lines) {
                writer.write(mapper.writeValueAsString(line));
                writer.newLine();
            }
        }
    }

    public List<JsonOutputLine> loadRunLog(String chash, String run) throws IOException {
        Path file = dirs.repoRunLog(this.name, chash, run);
        List<JsonOutputLine> lines = new ArrayList<>();
        for (String line : Files.readString(file).lines().toList()) {
            lines.add(mapper.readValue(line, JsonOutputLine.class));
        }
        return lines;
    }

    public void deleteRunLogs(String chash) throws IOException {
        Path dir = dirs.repoRunLogs(this.name, chash);
        FsUtil.removeDirRecursively(dir);
    }
}
