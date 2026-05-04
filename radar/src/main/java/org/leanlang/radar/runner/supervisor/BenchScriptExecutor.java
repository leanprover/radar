package org.leanlang.radar.runner.supervisor;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
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
            lines.addInternal(e);
            return -1;
        }
    }

    private int runBenchScript() throws Exception {
        log.info("Running bench script");

        Path repoPath = dirs.tmpRepo().toAbsolutePath();
        Path benchRepoPath = dirs.tmpBenchRepo().toAbsolutePath();
        Path outPath = dirs.tmpResultFile().toAbsolutePath();
        Path cachePath = dirs.repoCache(job.repo()).toAbsolutePath();

        ProcessBuilder builder = new ProcessBuilder(
                        dirs.tmpBenchRepoScript(job.script()).toAbsolutePath().toString(),
                        repoPath.toString(),
                        outPath.toString())
                .directory(benchRepoPath.toFile());

        Map<String, String> env = builder.environment();
        env.put("RADAR_REPO", repoPath.toString());
        env.put("RADAR_BENCH_REPO", benchRepoPath.toString());
        env.put("RADAR_OUT", outPath.toString());
        env.put("RADAR_CACHE", cachePath.toString());

        Process process = builder.start();

        int exitCode;
        try (BufferedReader stdoutReader = process.inputReader(StandardCharsets.UTF_8);
                BufferedReader stderrReader = process.errorReader(StandardCharsets.UTF_8)) {

            log.info("Listening to bench script output");
            Thread stdoutThread = new Thread(() -> stdoutReader.lines().forEach(lines::addStdout), "stdout");
            Thread stderrThread = new Thread(() -> stderrReader.lines().forEach(lines::addStderr), "stderr");

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
