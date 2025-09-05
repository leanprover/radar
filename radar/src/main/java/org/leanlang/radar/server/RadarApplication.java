package org.leanlang.radar.server;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.leanlang.radar.server.api.ResDebug;

public class RadarApplication extends Application<RadarConfiguration> {

    public static void main(final String[] args) throws Exception {
        new RadarApplication().run(args);
    }

    @Override
    public String getName() {
        return "Radar";
    }

    @Override
    public void initialize(final Bootstrap<RadarConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final RadarConfiguration configuration, final Environment environment) {
        environment.jersey().register(new ResDebug());
    }
}
