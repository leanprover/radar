package org.leanlang.radar.server;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.client.Client;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.api.ResAdminEnqueue;
import org.leanlang.radar.server.api.ResCommit;
import org.leanlang.radar.server.api.ResCommitRun;
import org.leanlang.radar.server.api.ResCompare;
import org.leanlang.radar.server.api.ResQueue;
import org.leanlang.radar.server.api.ResQueueRun;
import org.leanlang.radar.server.api.ResQueueRunnerFinish;
import org.leanlang.radar.server.api.ResQueueRunnerStatus;
import org.leanlang.radar.server.api.ResQueueRunnerTake;
import org.leanlang.radar.server.api.ResRepoGithubBot;
import org.leanlang.radar.server.api.ResRepoGithubWebhook;
import org.leanlang.radar.server.api.ResRepoGraph;
import org.leanlang.radar.server.api.ResRepoHistory;
import org.leanlang.radar.server.api.ResRepos;
import org.leanlang.radar.server.api.auth.Admin;
import org.leanlang.radar.server.api.auth.AdminAuthenticator;
import org.leanlang.radar.server.busser.Busser;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfig;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repos;
import org.leanlang.radar.server.runners.Runners;

public final class ServerApplication extends Application<ServerConfig> {
    public static final String NAME = "Radar Server";

    private final Path configFile;
    private final @Nullable Path stateDir;
    private final @Nullable Path cacheDir;
    private final @NonNull Map<String, Path> githubPatFiles;

    public ServerApplication(
            Path configFile,
            @Nullable Path stateDir,
            @Nullable Path cacheDir,
            @Nullable Map<String, Path> githubPatFiles) {
        this.configFile = configFile;
        this.stateDir = stateDir;
        this.cacheDir = cacheDir;
        this.githubPatFiles = githubPatFiles == null ? Map.of() : githubPatFiles;
    }

    public void run() throws Exception {
        run("server", configFile.toString());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize(Bootstrap<ServerConfig> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/radar-ui/", "/", "index.html", "ui-assets"));
        bootstrap.addBundle(new NotFoundRedirectBundle("ui-assets", "/index.html", "ui-assets-redirect"));
    }

    @Override
    public void run(ServerConfig configuration, Environment environment) throws IOException {
        configureDummyHealthCheck(environment);
        configureAdminAuth(environment, configuration.adminToken);
        var client = configureJerseyClient(configuration, environment);

        var dirs = new Dirs(configFile, stateDir, cacheDir, configuration.dirs);
        var repos = new Repos(environment, client, dirs, configuration.repos, githubPatFiles);
        var runners = new Runners(configuration.runners);
        var queue = new Queue(repos, runners);
        var busser = new Busser(repos, queue);

        environment.lifecycle().manage(repos);
        environment.lifecycle().manage(busser);

        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new ResAdminEnqueue(repos, queue));
        environment.jersey().register(new ResCommit(repos, queue));
        environment.jersey().register(new ResCommitRun(repos, queue));
        environment.jersey().register(new ResCompare(repos));
        environment.jersey().register(new ResQueue(repos, runners, queue));
        environment.jersey().register(new ResQueueRun(queue));
        environment.jersey().register(new ResQueueRunnerFinish(runners, queue, busser));
        environment.jersey().register(new ResQueueRunnerStatus(runners, queue));
        environment.jersey().register(new ResQueueRunnerTake(runners, queue));
        environment.jersey().register(new ResRepoGithubBot(repos));
        environment.jersey().register(new ResRepoGithubWebhook(repos, busser));
        environment.jersey().register(new ResRepoGraph(repos));
        environment.jersey().register(new ResRepoHistory(repos));
        environment.jersey().register(new ResRepos(repos));
    }

    private static void configureDummyHealthCheck(Environment environment) {
        environment.healthChecks().register("dummy", new HealthCheck() {
            @Override
            protected Result check() {
                return Result.healthy();
            }
        });
    }

    private static void configureAdminAuth(Environment environment, String adminToken) {
        environment
                .jersey()
                .register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<Admin>()
                        .setAuthenticator(new AdminAuthenticator(adminToken))
                        .buildAuthFilter()));

        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(Admin.class));
    }

    private Client configureJerseyClient(ServerConfig configuration, Environment environment) {
        return new JerseyClientBuilder(environment)
                .using(configuration.jerseyClient)
                .build(getName());
    }
}
