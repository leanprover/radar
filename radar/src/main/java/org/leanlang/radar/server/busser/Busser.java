package org.leanlang.radar.server.busser;

import io.dropwizard.lifecycle.Managed;
import java.time.Instant;
import java.util.List;
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
        executor.scheduleWithFixedDelay(this::doUpdateAll, 1, Constants.BUSSER_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        executor.close();
    }

    public void updateRepo(String repoName) {
        Repo repo = repos.repo(repoName);
        executor.execute(() -> doUpdateRepo(repo));
    }

    public void updateGhRepliesForRepo(String repoName) {
        Repo repo = repos.repo(repoName);
        executor.execute(() -> doUpdateGhReplies(repo));
    }

    public void vacuumRepo(String repoName) {
        Repo repo = repos.repo(repoName);
        executor.execute(() -> doVacuumRepo(repo));
    }

    // The following methods all have the prefix "do" so they don't collide with the public names.

    private synchronized void doUpdateAll() {
        for (Repo repo : repos.repos()) {
            doUpdateRepo(repo);
        }
    }

    private synchronized void doUpdateRepo(Repo repo) {
        try {
            log.info("Updating repo {}", repo.name());
            doUpdateRepoImpl(repo);
        } catch (Exception e) {
            log.error("Failed to update repo {}", repo.name(), e);
        }
    }

    private synchronized void doUpdateRepoImpl(Repo repo) throws GitAPIException {
        if (repo.gh().isEmpty()) {
            doFetch(repo);
            return;
        }

        GhUpdater ghUpdater = new GhUpdater(repo, queue, repo.gh().get());
        Instant since = ghUpdater.since();
        List<JsonGhComment> comments = ghUpdater.searchForComments(since);

        // We must fetch before we process the bench commands to ensure we're aware of every commit involved.
        doFetch(repo);

        ghUpdater.addCommands(comments, since);
        ghUpdater.executeCommands();
        ghUpdater.updateReplies();
    }

    /**
     * Fetch commits from GitHub and update the DB accordingly.
     */
    private synchronized void doFetch(Repo repo) throws GitAPIException {
        repo.git().fetch();
        repo.gitBench().fetch();

        DbUpdater dbUpdater = new DbUpdater(repo);
        dbUpdater.update(queue);
    }

    private synchronized void doUpdateGhReplies(Repo repo) {
        if (repo.gh().isEmpty()) return;
        log.info("Updating gh replies for repo {}", repo.name());
        GhUpdater ghUpdater = new GhUpdater(repo, queue, repo.gh().get());
        ghUpdater.executeCommands(); // Otherwise the reply bodies won't be up-to-date.
        ghUpdater.updateReplies();
    }

    private synchronized void doVacuumRepo(Repo repo) {
        log.info("Vacuuming repo {}", repo.name());
        new DbUpdater(repo).runVacuum();
    }
}
