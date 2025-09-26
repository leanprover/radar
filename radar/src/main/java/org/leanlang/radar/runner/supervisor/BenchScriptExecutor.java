package org.leanlang.radar.runner.supervisor;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.leanlang.radar.runner.config.Dirs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BenchScriptExecutor implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(BenchScriptExecutor.class);

    private final Dirs dirs;
    private final JsonJob job;
    private final OutputLines lines;

    private final ExecutorService executor;
    private final Future<Integer> result;

    public BenchScriptExecutor(Dirs dirs, JsonJob job, OutputLines lines) {
        this.dirs = dirs;
        this.job = job;
        this.lines = lines;

        executor = Executors.newSingleThreadExecutor();
        result = executor.submit(this::executeBenchmark);
    }

    @Override
    public void close() {
        executor.close();
    }

    private int executeBenchmark() {
        try {
            return runBenchScript();
        } catch (Exception e) {
            lines.add(e);
            return -1;
        }
    }

    private int runBenchScript() throws Exception {
        log.info("Running bench script");
        Process process = new ProcessBuilder(
                        dirs.tmpBenchRepoScript(job.script()).toAbsolutePath().toString(),
                        dirs.tmpRepo().toAbsolutePath().toString(),
                        dirs.tmpResultFile().toAbsolutePath().toString())
                .directory(dirs.tmpBenchRepo().toFile())
                .start();

        int exitCode;
        try (BufferedReader stdoutReader = process.inputReader(StandardCharsets.UTF_8);
                BufferedReader stderrReader = process.errorReader(StandardCharsets.UTF_8)) {

            log.info("Listening to bench script output");
            Thread stdoutThread = new Thread(
                    () -> stdoutReader.lines().forEach(line -> lines.add(OutputLine.STDOUT, line)), "stdout");
            Thread stderrThread = new Thread(
                    () -> stderrReader.lines().forEach(line -> lines.add(OutputLine.STDERR, line)), "stderr");

            stdoutThread.start();
            stderrThread.start();

            exitCode = process.waitFor();
            log.info("Bench script exited with {}", exitCode);

            stdoutThread.join();
            stderrThread.join();
        }

        log.info("Finished running bench script");
        return exitCode;
    }

    public int result() throws ExecutionException, InterruptedException {
        return result.get();
    }
}
