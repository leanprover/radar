package org.leanlang.radar.server.busser;

import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.leanlang.radar.Constants;
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
        executor.scheduleWithFixedDelay(this::update, 1, Constants.BUSSER_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        executor.close();
    }

    public void updateOnce() {
        executor.execute(this::update);
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
        repo.git().fetch();
        repo.gitBench().fetch();

        DbUpdater updater = new DbUpdater(repo);
        updater.updateRepoData();
        updater.updateQueue();
    }
}
