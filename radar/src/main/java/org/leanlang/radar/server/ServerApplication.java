package org.leanlang.radar.server;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.io.IOException;
import java.nio.file.Path;
import org.leanlang.radar.server.api.ResDebug;
import org.leanlang.radar.server.api.ResQueue;
import org.leanlang.radar.server.api.ResRepos;
import org.leanlang.radar.server.api.ResReposNameCommitsChash;
import org.leanlang.radar.server.api.ResReposNameHistory;
import org.leanlang.radar.server.api.ResRunners;
import org.leanlang.radar.server.busser.Busser;
import org.leanlang.radar.server.config.Dirs;
import org.leanlang.radar.server.config.ServerConfig;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.runners.Runners;

public final class ServerApplication extends Application<ServerConfig> {
    public static final String NAME = "Radar Server";

    private final Path configFile;

    public ServerApplication(Path configFile) {
        this.configFile = configFile;
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

        var dirs = new Dirs(configFile, configuration.dirs);
        var repos = new Repos(dirs, configuration.repos);
        var runners = new Runners(configuration.runners);
        var busser = new Busser(repos);

        environment.lifecycle().manage(repos);
        environment.lifecycle().manage(busser);

        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new ResDebug(runners));
        environment.jersey().register(new ResQueue(runners));
        environment.jersey().register(new ResRepos(repos));
        environment.jersey().register(new ResReposNameCommitsChash(repos));
        environment.jersey().register(new ResReposNameHistory(repos));
        environment.jersey().register(new ResRunners(runners));
    }

    private static void configureDummyHealthCheck(Environment environment) {
        environment.healthChecks().register("dummy", new HealthCheck() {
            @Override
            protected Result check() {
                return Result.healthy();
            }
        });
    }
}
