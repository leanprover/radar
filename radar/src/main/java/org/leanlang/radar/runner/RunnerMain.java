package org.leanlang.radar.runner;

import ch.qos.logback.classic.Level;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.logging.common.BootstrapLogging;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.logging.common.LoggingFactory;
import jakarta.ws.rs.client.Client;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.leanlang.radar.runner.supervisor.Supervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RunnerMain {
    public static final String NAME = "Radar Runner";
    private static final Logger log = LoggerFactory.getLogger(RunnerMain.class);

    private final RunnerConfig config;
    private final Client client;

    public RunnerMain(Path configFile) throws Exception {
        // Startup process inspired by - and using - Dropwizard machinery

        BootstrapLogging.bootstrap(Level.WARN); // No hibernate debug prints, please

        Environment environment = new Environment(
                NAME,
                Jackson.newObjectMapper(),
                Validators.newValidatorFactory(),
                new MetricRegistry(),
                Thread.currentThread().getContextClassLoader(),
                new HealthCheckRegistry(),
                new Configuration());

        config = loadConfig(configFile);
        configureLogging(config, environment.metrics());

        client = new JerseyClientBuilder(environment).build(NAME);
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

    private static void configureLogging(RunnerConfig config, MetricRegistry metricRegistry) {
        LoggingFactory loggingFactory = config.logging();
        if (loggingFactory == null) {
            loggingFactory = new DefaultLoggingFactory();
        }
        loggingFactory.configure(metricRegistry, NAME);
    }

    public void run() {
        Supervisor supervisor = new Supervisor(config, client);
        StatusUpdater statusUpdater = new StatusUpdater(config, supervisor, client);

        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.scheduleWithFixedDelay(statusUpdater::run, 0, 1, TimeUnit.SECONDS);

            while (true) {
                try {
                    while (supervisor.run())
                        ;
                } catch (Exception e) {
                    log.error(e.getMessage());
                }

                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
