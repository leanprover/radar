package org.leanlang.radar.server;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.leanlang.radar.server.api.ResDebug;
import org.leanlang.radar.server.api.ResRepos;
import org.leanlang.radar.server.config.ServerConfig;

public class ServerApplication extends Application<ServerConfig> {
    public static final String NAME = "Radar Server";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize(final Bootstrap<ServerConfig> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/radar-ui/", "/", "index.html", "ui-assets"));
        bootstrap.addBundle(new NotFoundRedirectBundle("ui-assets", "/index.html", "ui-assets-redirect"));
    }

    @Override
    public void run(final ServerConfig configuration, final Environment environment) {
        configureDummyHealthCheck(environment);

        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new ResDebug());
        environment.jersey().register(new ResRepos(configuration.repos));
    }

    private static void configureDummyHealthCheck(final Environment environment) {
        environment.healthChecks().register("dummy", new HealthCheck() {
            @Override
            protected Result check() {
                return Result.healthy();
            }
        });
    }
}
