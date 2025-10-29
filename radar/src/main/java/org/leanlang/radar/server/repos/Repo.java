package org.leanlang.radar.server.repos;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.client.Client;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.FsUtil;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;
import org.leanlang.radar.server.config.ServerConfigRepoRun;
import org.leanlang.radar.server.config.credentials.GithubCredentials;
import org.leanlang.radar.server.config.credentials.ZulipCredentials;
import org.leanlang.radar.server.repos.source.RepoSource;
import org.leanlang.radar.server.repos.source.RepoSourceGithub;

public final class Repo implements AutoCloseable {
    private final Environment environment;
    private final Dirs dirs;
    private final ServerConfigRepo config;

    private final RepoSource source;
    private final RepoSource benchSource;
    private final List<RepoMetricMatcher> metricMatchers;

    private final RepoDb db;
    private final RepoGit git;
    private final RepoGit gitBench;
    private final @Nullable RepoGh gh;
    private final @Nullable RepoZulip zulip;

    public Repo(
            Environment environment,
            Client client,
            Dirs dirs,
            ServerConfigRepo config,
            @Nullable GithubCredentials githubCredentials,
            @Nullable ZulipCredentials zulipCredentials)
            throws IOException {

        this.environment = environment;
        this.dirs = dirs;
        this.config = config;

        this.source = RepoSource.parse(config.url);
        this.benchSource = RepoSource.parse(config.benchUrl);
        this.metricMatchers = Optional.ofNullable(config.metrics).stream()
                .flatMap(Collection::stream)
                .map(RepoMetricMatcher::new)
                .toList();

        this.db = new RepoDb(name(), dirs.repoDb(name()));
        this.git = new RepoGit(dirs.repoGit(name()), this.source.gitUrl());
        this.gitBench = new RepoGit(dirs.repoGitBench(name()), this.benchSource.gitUrl());
        this.gh = mkRepoGh(client, this.source, githubCredentials);
        this.zulip = mkRepoZulip(client, config, zulipCredentials);
    }

    private static @Nullable RepoGh mkRepoGh(
            Client client, RepoSource source, @Nullable GithubCredentials credentials) {
        if (!(source instanceof RepoSourceGithub(String owner, String repo))) return null;
        if (credentials == null) return null;
        return new RepoGh(client, owner, repo, credentials);
    }

    private static @Nullable RepoZulip mkRepoZulip(
            Client client, ServerConfigRepo config, @Nullable ZulipCredentials credentials) {
        if (credentials == null) return null;
        return new RepoZulip(client, config.zulip, credentials);
    }

    @Override
    public void close() {
        db.close();
        git.close();
        gitBench.close();
    }

    public String name() {
        return config.name;
    }

    public String description() {
        return config.description;
    }

    public RepoSource source() {
        return source;
    }

    public String ref() {
        return config.ref;
    }

    public Optional<String> lakeprofReportUrl() {
        return Optional.ofNullable(config.lakeprofReportUrl);
    }

    public RepoSource benchSource() {
        return benchSource;
    }

    public String benchRef() {
        return config.benchRef;
    }

    public List<ServerConfigRepoRun> benchRuns() {
        return config.benchRuns;
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

    public Optional<RepoGh> gh() {
        return Optional.ofNullable(gh);
    }

    public Optional<RepoZulip> zulip() {
        return Optional.ofNullable(zulip);
    }

    public RepoMetricMetadata metricMetadata(String name) {
        RepoMetricMetadata result = new RepoMetricMetadata();
        for (RepoMetricMatcher matcher : metricMatchers) {
            if (matcher.matches(name)) {
                result = matcher.update(result);
            }
        }
        return result;
    }

    public int significantMajorMetrics() {
        return config.significantMajorMetrics;
    }

    public int significantMinorMetrics() {
        return config.significantMinorMetrics;
    }

    public void saveRunLog(String chash, String run, List<JsonOutputLine> lines) throws IOException {
        Path file = dirs.repoRunLog(name(), chash, run);
        Files.createDirectories(file.getParent());
        ObjectMapper mapper = environment.getObjectMapper();
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE)) {
            for (JsonOutputLine line : lines) {
                writer.write(mapper.writeValueAsString(line));
                writer.newLine();
            }
        }
    }

    public List<JsonOutputLine> loadRunLog(String chash, String run) throws IOException {
        Path file = dirs.repoRunLog(name(), chash, run);
        ObjectMapper mapper = environment.getObjectMapper();
        List<JsonOutputLine> lines = new ArrayList<>();
        for (String line : Files.readString(file).lines().toList()) {
            lines.add(mapper.readValue(line, JsonOutputLine.class));
        }
        return lines;
    }

    public void deleteRunLogs(String chash) throws IOException {
        Path dir = dirs.repoRunLogs(name(), chash);
        FsUtil.removeDirRecursively(dir);
    }
}
