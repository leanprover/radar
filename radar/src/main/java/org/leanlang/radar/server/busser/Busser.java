package org.leanlang.radar.server.busser;

import io.dropwizard.lifecycle.Managed;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.leanlang.radar.Constants;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server's assistant.
 */
public final class Busser implements Managed {
    private static final Logger log = LoggerFactory.getLogger(Busser.class);

    private final Repos repos;
    private final Queue queue;
    private final ScheduledExecutorService executor;

    public Busser(Repos repos, Queue queue) {
        this.repos = repos;
        this.queue = queue;
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
        DbUpdater dbUpdater = new DbUpdater(repo, queue);
        Optional<GhUpdater> ghUpdaterOpt = repo.gh().map(it -> new GhUpdater(repo, queue, it));

        if (ghUpdaterOpt.isPresent()) {
            GhUpdater ghUpdater = ghUpdaterOpt.get();
            Instant since = ghUpdater.since();
            List<JsonGhComment> comments = ghUpdater.searchForComments(since);

            // We must fetch before we process the bench commands to ensure we're aware of every commit involved.
            repo.git().fetch();
            repo.gitBench().fetch();
            dbUpdater.update();

            ghUpdater.addCommands(comments, since);
            ghUpdater.executeCommands();
            ghUpdater.updateReplies();
        } else {
            repo.git().fetch();
            repo.gitBench().fetch();
            dbUpdater.update();
        }
    }
}
