package org.leanlang.radar.server.busser;

import io.dropwizard.lifecycle.Managed;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.Constants;
import org.leanlang.radar.Formatter;
import org.leanlang.radar.Linker;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoGh;
import org.leanlang.radar.server.repos.RepoZulip;
import org.leanlang.radar.server.repos.Repos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server's assistant.
 */
public final class Busser implements Managed {
    private static final Logger log = LoggerFactory.getLogger(Busser.class);

    private final Linker linker;
    private final Repos repos;
    private final Queue queue;
    private final ScheduledExecutorService executor;

    public Busser(Linker linker, Repos repos, Queue queue) {
        this.linker = linker;
        this.repos = repos;
        this.queue = queue;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        // Frequently update the repository
        executor.scheduleWithFixedDelay(
                this::doUpdateAll, 1, Constants.BUSSER_UPDATE_DELAY.toMillis(), TimeUnit.MILLISECONDS);

        // Infrequently perform more aggressive cleanup
        long secondsPerDay = 24 * 60 * 60;
        long secondsElapsedToday =
                Instant.now().atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay();
        long secondsToWait =
                (secondsPerDay + Constants.BUSSER_MAINTENANCE_DELAY.toSeconds() - secondsElapsedToday) % secondsPerDay;
        log.info("Maintaining repos in {}", new Formatter().formatValueWithUnit(secondsToWait, "s"));
        executor.schedule(
                () -> executor.scheduleAtFixedRate(this::doMaintainAll, 0, secondsPerDay, TimeUnit.SECONDS),
                secondsToWait,
                TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        executor.shutdownNow();
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

    public void maintainRepo(String repoName, boolean aggressive) {
        Repo repo = repos.repo(repoName);
        executor.execute(() -> doMaintainRepo(repo, aggressive));
    }

    public void recomputeSignificance(String repoName) {
        Repo repo = repos.repo(repoName);
        executor.execute(() -> doRecomputeSignificance(repo));
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
            log.info("Updated repo {}", repo.name());
        } catch (Exception e) {
            log.error("Failed to update repo {}", repo.name(), e);
        }
    }

    private synchronized void doUpdateRepoImpl(Repo repo) throws GitAPIException {
        GithubBotUpdater githubBotUpdater = githubBotUpdater(repo);
        ZulipBotUpdater zulipBotUpdater = zulipBotUpdater(repo);

        if (githubBotUpdater != null) githubBotUpdater.fetch();
        new RepoDataUpdater(repo).update();
        if (githubBotUpdater != null) githubBotUpdater.update();
        new QueueUpdater(repo, queue).update();
        new SignificanceUpdater(repo).update();
        if (zulipBotUpdater != null) zulipBotUpdater.update();
    }

    private @Nullable GithubBotUpdater githubBotUpdater(Repo repo) {
        RepoGh repoGh = repo.gh().orElse(null);
        if (repoGh == null) return null;
        return new GithubBotUpdater(linker, repo, queue, repoGh);
    }

    private @Nullable ZulipBotUpdater zulipBotUpdater(Repo repo) {
        RepoZulip repoZulip = repo.zulip().orElse(null);
        if (repoZulip == null) return null;
        String channel = repoZulip.config().feedChannel();
        String topic = repoZulip.config().feedTopic();
        if (channel == null) return null;
        if (topic == null) return null;
        return new ZulipBotUpdater(linker, repo, repoZulip, channel, topic);
    }

    private synchronized void doUpdateGhReplies(Repo repo) {
        if (repo.gh().isEmpty()) return;
        new GithubBotUpdater(linker, repo, queue, repo.gh().get()).update();
    }

    private synchronized void doMaintainAll() {
        for (Repo repo : repos.repos()) {
            doMaintainRepo(repo, false);
        }
    }

    private synchronized void doMaintainRepo(Repo repo, boolean aggressive) {
        new RepoMaintainer(repo).maintain(aggressive);
    }

    private synchronized void doRecomputeSignificance(Repo repo) {
        SignificanceUpdater significanceUpdater = new SignificanceUpdater(repo);
        significanceUpdater.clearAll();
        significanceUpdater.update();
    }
}
