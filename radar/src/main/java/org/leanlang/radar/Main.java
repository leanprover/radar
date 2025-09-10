package org.leanlang.radar;

import java.nio.file.Path;
import org.leanlang.radar.runner.RunnerMain;
import org.leanlang.radar.server.ServerApplication;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command()
public final class Main {

    @Command(name = "server", description = "Start the server.")
    void runServer(@Parameters(index = "0", description = "Path to the config file.") Path configFile)
            throws Exception {
        new ServerApplication(configFile).run();
    }

    @Command(name = "runner", description = "Start the runner.")
    void runRunner(@Parameters(index = "0", description = "Path to the config file.") Path configFile)
            throws Exception {
        new RunnerMain(configFile).run();
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
