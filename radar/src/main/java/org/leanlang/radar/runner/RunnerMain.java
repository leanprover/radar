package org.leanlang.radar.runner;

import ch.qos.logback.classic.Level;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.logging.common.BootstrapLogging;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.logging.common.LoggingFactory;
import java.io.IOException;
import java.nio.file.Path;

public class RunnerMain {
    public static final String NAME = "Radar Runner";

    final RunnerConfig config;

    public RunnerMain(Path configFile) throws Exception {
        BootstrapLogging.bootstrap(Level.WARN); // No hibernate debug prints, please
        config = loadConfig(configFile);
        configureLogging(config);
    }

    private static RunnerConfig loadConfig(Path configFile) throws IOException, ConfigurationException {
        // Should work the same as Dropwizard's config loading, including validation.
        return new DefaultConfigurationFactoryFactory<RunnerConfig>()
                .create(
                        RunnerConfig.class,
                        Validators.newValidatorFactory().getValidator(),
                        Jackson.newObjectMapper(),
                        "dw")
                .build(new FileConfigurationSourceProvider(), configFile.toString());
    }

    private static void configureLogging(RunnerConfig config) {
        LoggingFactory loggingFactory = config.logging();
        if (loggingFactory == null) {
            loggingFactory = new DefaultLoggingFactory();
        }
        loggingFactory.configure(new MetricRegistry(), NAME);
    }

    public void run() {}
}
