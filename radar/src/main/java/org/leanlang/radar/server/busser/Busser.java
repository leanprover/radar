package org.leanlang.radar.server.busser;

import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server's assistant.
 */
public final class Busser implements Managed {
    private static final Logger log = LoggerFactory.getLogger(Busser.class);
    private final Repos repos;
    private final ScheduledExecutorService executor;

    public Busser(Repos repos) {
        this.repos = repos;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        executor.scheduleWithFixedDelay(this::update, 1, 30 * 60, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        executor.close();
    }

    private synchronized void update() {
        updateRepos();
    }

    private void updateRepos() {
        for (Repo repo : repos.repos()) {
            try {
                log.info("Updating repo {}", repo.name());
                updateRepo(repo);
            } catch (Exception e) {
                log.error("Failed to update repo {}", repo.name(), e);
            }
        }
    }

    private void updateRepo(Repo repo) throws GitAPIException {
        DbUpdater updater = new DbUpdater(repo);

        log.debug("Step 1: Fetch new commits");
        repo.git().fetch();

        log.debug("Step 2: Update repo info in db");
        updater.updateRepoData();

        log.debug("Step 3: Update queue");
    }
}
