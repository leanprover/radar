package org.leanlang.radar.server.repos;

import java.nio.file.Path;
import java.time.Duration;
import org.eclipse.jgit.lib.BatchingProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepoGitProgressMonitor extends BatchingProgressMonitor {
    private static final Logger log = LoggerFactory.getLogger(RepoGitProgressMonitor.class);
    private final String action;
    private final Path path;

    public RepoGitProgressMonitor(String action, Path path) {
        this.action = action;
        this.path = path;
    }

    @Override
    protected void onUpdate(String taskName, int workCurr, Duration duration) {
        log.info("{} repo {}: {}: ({})", action, path, taskName, workCurr);
    }

    @Override
    protected void onEndTask(String taskName, int workCurr, Duration duration) {
        log.info("{} repo {}: {}: ({}), done.", action, path, taskName, workCurr);
    }

    @Override
    protected void onUpdate(String taskName, int workCurr, int workTotal, int percentDone, Duration duration) {
        log.info("{} repo {}: {}: {}% ({}/{})", action, path, taskName, percentDone, workCurr, workTotal);
    }

    @Override
    protected void onEndTask(String taskName, int workCurr, int workTotal, int percentDone, Duration duration) {
        log.info("{} repo {}: {}: {}% ({}/{}), done.", action, path, taskName, percentDone, workCurr, workTotal);
    }
}
