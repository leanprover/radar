package org.leanlang.radar.server;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.leanlang.radar.server.api.ResDebug;
import org.leanlang.radar.server.api.ResRepos;
import org.leanlang.radar.server.config.RadarConfig;

public class RadarApplication extends Application<RadarConfig> {

    public static void main(final String[] args) throws Exception {
        new RadarApplication().run(args);
    }

    @Override
    public String getName() {
        return "Radar";
    }

    @Override
    public void initialize(final Bootstrap<RadarConfig> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/radar-ui/", "/", "index.html", "ui-assets"));
        bootstrap.addBundle(new NotFoundRedirectBundle("ui-assets", "/index.html", "ui-assets-redirect"));
    }

    @Override
    public void run(final RadarConfig configuration, final Environment environment) {
        environment.jersey().setUrlPattern("/api/*");
        environment.jersey().register(new ResDebug());
        environment.jersey().register(new ResRepos(configuration.repos));
    }
}
