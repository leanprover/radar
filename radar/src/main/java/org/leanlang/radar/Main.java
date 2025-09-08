package org.leanlang.radar;

import java.nio.file.Path;
import org.leanlang.radar.runner.RunnerMain;
import org.leanlang.radar.server.RadarApplication;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command()
public class Main {

    @Command(name = "server", description = "Start the server.")
    void runServer(@Parameters(index = "0", description = "Path to the config file.") final Path configFile)
            throws Exception {
        new RadarApplication().run("server", configFile.toString());
    }

    @Command(name = "runner", description = "Start the runner.")
    void runRunner(@Parameters(index = "0", description = "Path to the config file.") final Path configFile)
            throws Exception {
        new RunnerMain(configFile).run();
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
