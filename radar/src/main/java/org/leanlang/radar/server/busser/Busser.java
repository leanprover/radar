package org.leanlang.radar.server.busser;

import io.dropwizard.lifecycle.Managed;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.leanlang.radar.Constants;
import org.leanlang.radar.Formatter;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;
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
        // Frequently update the repository
        executor.scheduleWithFixedDelay(this::doUpdateAll, 1, Constants.BUSSER_DELAY.toMillis(), TimeUnit.MILLISECONDS);

        // Infrequently perform more aggressive cleanup
        int secondsPerDay = 24 * 60 * 60;
        int secondsAlreadyToday =
                Instant.now().atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();
        int secondsToWait = secondsPerDay - secondsAlreadyToday;
        log.info("Cleaning repos in {}", new Formatter().formatValueWithUnit(secondsToWait, "s"));
        executor.schedule(
                () -> executor.scheduleAtFixedRate(this::doCleanAll, 0, secondsPerDay, TimeUnit.SECONDS),
                secondsToWait,
                TimeUnit.SECONDS);
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
        GhUpdater ghUpdater =
                repo.gh().map(it -> new GhUpdater(repo, queue, it)).orElse(null);

        if (ghUpdater != null) ghUpdater.fetch();
        new RepoDataUpdater(repo).update();
        if (ghUpdater != null) ghUpdater.update();
        new QueueUpdater(repo, queue).update();
    }

    private synchronized void doUpdateGhReplies(Repo repo) {
        if (repo.gh().isEmpty()) return;
        log.info("Updating gh replies for repo {}", repo.name());
        new GhUpdater(repo, queue, repo.gh().get()).update();
    }

    private synchronized void doCleanAll() {
        for (Repo repo : repos.repos()) {
            doCleanRepo(repo);
        }
    }

    private synchronized void doCleanRepo(Repo repo) {
        log.info("Cleaning repo {}", repo.name());

        // Run `pragma optimize` on the DB
        // https://sqlite.org/lang_analyze.html#periodically_run_pragma_optimize_
        log.info("Running pragma optimize");
        repo.db().writeTransaction(ctx -> ctx.dsl().execute("PRAGMA optimize"));
        log.info("Ran pragma optimize");

        // Garbage collect the bare git repo
        try {
            repo.git().gc();
        } catch (GitAPIException e) {
            log.error("Failed to gc repo {}", repo.name(), e);
        }
    }

    private synchronized void doVacuumRepo(Repo repo) {
        log.info("Vacuuming repo {}", repo.name());
        try {
            repo.db().writeWithoutTransactionDoNotUseUnlessYouKnowWhatYouAreDoing(ctx -> ctx.dsl()
                    .execute("VACUUM"));
        } catch (Throwable e) {
            log.error("Failed to vacuum", e);
        }
        log.info("Vacuumed repo {}", repo.name());
    }
}
