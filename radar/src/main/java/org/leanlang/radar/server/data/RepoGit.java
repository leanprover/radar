package org.leanlang.radar.server.data;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BatchingProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RepoGit implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(RepoGit.class);

    private final Path path;
    private final URI url;
    private final Repository repo;
    private final Git git;

    public RepoGit(Path path, URI url) throws IOException {
        log.info("Opening repo {}", path);
        this.path = path;
        this.url = url;

        Files.createDirectories(path.getParent());

        repo = new FileRepositoryBuilder().setGitDir(path.toFile()).setBare().build();
        git = new Git(repo);

        try {
            repo.create(true);
        } catch (IllegalStateException e) {
            if (!e.getMessage().startsWith("Repository already exists:")) throw e;
        }
    }

    @Override
    public void close() {
        log.info("Closing repo {}", path);
        repo.close();
    }

    public void fetch() throws GitAPIException {
        log.info("Fetching repo {} (this may take a while)", path);
        git.fetch()
                .setRemote(url.toString())
                .setRefSpecs(new RefSpec("+refs/heads/*:refs/heads/*"))
                .setTagOpt(TagOpt.NO_TAGS)
                .setForceUpdate(true)
                .setRemoveDeletedRefs(true)
                .setProgressMonitor(new BatchingProgressMonitor() {
                    @Override
                    protected void onUpdate(String taskName, int workCurr, Duration duration) {
                        log.info("Fetching repo {}: {}: ({})", path, taskName, workCurr);
                    }

                    @Override
                    protected void onEndTask(String taskName, int workCurr, Duration duration) {
                        log.info("Fetching repo {}: {}: ({}), done.", path, taskName, workCurr);
                    }

                    @Override
                    protected void onUpdate(
                            String taskName, int workCurr, int workTotal, int percentDone, Duration duration) {
                        log.info("Fetching repo {}: {}: {}% ({}/{})", path, taskName, percentDone, workCurr, workTotal);
                    }

                    @Override
                    protected void onEndTask(
                            String taskName, int workCurr, int workTotal, int percentDone, Duration duration) {
                        log.info(
                                "Fetching repo {}: {}: {}% ({}/{}), done.",
                                path, taskName, percentDone, workCurr, workTotal);
                    }
                })
                .call();
        log.info("Finished fetching repo {}", path);
    }

    public Repository plumbing() {
        return repo;
    }

    public Git porcelain() {
        return git;
    }
}
