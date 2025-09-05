package org.leanlang.radar;

import org.leanlang.radar.server.RadarApplication;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command()
public class Main {

    @Command(name = "server", description = "Start the server.")
    void runServer(@Parameters(index = "0", description = "Path to the config file.") String configFile)
            throws Exception {
        RadarApplication.main(new String[] {"server", configFile});
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
