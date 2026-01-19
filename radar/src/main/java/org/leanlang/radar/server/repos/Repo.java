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
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.runner.supervisor.JsonOutputLine;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfigRepo;
import org.leanlang.radar.server.config.ServerConfigRepoMetricFilter;
import org.leanlang.radar.server.config.ServerConfigRepoRun;
import org.leanlang.radar.server.config.credentials.GithubCredentials;
import org.leanlang.radar.server.config.credentials.ZulipCredentials;
import org.leanlang.radar.server.repos.source.RepoSource;
import org.leanlang.radar.server.repos.source.RepoSourceGithub;
import org.leanlang.radar.util.FsUtil;

public final class Repo implements AutoCloseable {
    private final Environment environment;
    private final Dirs dirs;
    private final ServerConfigRepo config;

    private final RepoSource source;
    private final RepoSource benchSource;
    private final List<ServerConfigRepoMetricFilter> metricFilters;

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
        this.metricFilters = Optional.ofNullable(config.significantMetrics).orElse(List.of()).stream()
                .toList();

        this.db = new RepoDb(name(), dirs.repoDb(name()));
        this.git = new RepoGit(dirs.repoGit(name()), this.source.gitUrl());
        this.gitBench = new RepoGit(dirs.repoGitBench(name()), this.benchSource.gitUrl());
        this.gh = mkRepoGh(client, config, this.source, githubCredentials);
        this.zulip = mkRepoZulip(client, config, zulipCredentials);
    }

    private static @Nullable RepoGh mkRepoGh(
            Client client, ServerConfigRepo config, RepoSource source, @Nullable GithubCredentials credentials) {
        if (!(source instanceof RepoSourceGithub(String owner, String repo))) return null;
        if (credentials == null) return null;
        return new RepoGh(client, config.github, owner, repo, credentials);
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

    public boolean refRegex() {
        return config.refRegex;
    }

    public boolean refParentsNone() {
        return config.refParentsNone;
    }

    public boolean refParentsFirst() {
        return config.refParentsFirst;
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

    public ServerConfigRepoMetricFilter metricFilter(String metric) {
        for (ServerConfigRepoMetricFilter filter : metricFilters) {
            if (filter.match.matcher(metric).find()) {
                return filter;
            }
        }
        return new ServerConfigRepoMetricFilter();
    }

    public int significantLargeChanges() {
        return config.significantLargeChanges;
    }

    public int significantMediumChanges() {
        return config.significantMediumChanges;
    }

    public int significantSmallChanges() {
        return config.significantSmallChanges;
    }

    public boolean significantRunFailures() {
        return config.significantRunFailures;
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
