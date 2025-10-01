package org.leanlang.radar;

import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.runner.RunnerMain;
import org.leanlang.radar.server.ServerApplication;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command()
public final class Main {

    @Command(name = "server", description = "Start the server.")
    void runServer(
            @Parameters(index = "0", description = "Path to the config file.", paramLabel = "<config-file>")
                    Path configFile,
            @Option(names = "--state-dir", description = "Path to the state dir.", paramLabel = "<path>") @Nullable
                    Path stateDir,
            @Option(names = "--cache-dir", description = "Path to the cache dir.", paramLabel = "<path>") @Nullable
                    Path cacheDir,
            @Option(
                            names = "--github-pat-file",
                            description = "Mapping from repo to GitHub personal access token files.",
                            paramLabel = "<repo>=<path>")
                    Map<String, Path> githubPatFiles)
            throws Exception {
        new ServerApplication(configFile, stateDir, cacheDir, githubPatFiles).run();
    }

    @Command(name = "runner", description = "Start the runner.")
    void runRunner(
            @Parameters(index = "0", description = "Path to the config file.", paramLabel = "<config-file>")
                    Path configFile,
            @Option(names = "--cache-dir", description = "Path to the cache dir.", paramLabel = "<path>") @Nullable
                    Path cacheDir,
            @Option(names = "--tmp-dir", description = "Path to the tmp dir.", paramLabel = "<path>") @Nullable
                    Path tmpDir)
            throws Exception {
        new RunnerMain(configFile, cacheDir, tmpDir).run();
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
