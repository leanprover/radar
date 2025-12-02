package org.leanlang.radar.server.repos;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RepoGit implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(RepoGit.class);

    private final Path path;
    private final @Nullable URI url;
    private final Repository repo;
    private final Git git;

    public RepoGit(Path path, @Nullable URI url) throws IOException {
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

    public Repository plumbing() {
        return repo;
    }

    public Git porcelain() {
        return git;
    }

    public ObjectId resolveRef(String ref) throws IOException {
        ObjectId id = plumbing().resolve(ref);
        if (id != null) return id;
        throw new IllegalArgumentException("Failed to resolve ref " + ref);
    }

    public ObjectId resolveRef(Ref ref) {
        ObjectId id = ref.getPeeledObjectId();
        if (id != null) return id;
        id = ref.getObjectId();
        if (id != null) return id;
        throw new IllegalArgumentException("Failed to resolve ref " + ref);
    }

    public void fetch() throws GitAPIException {
        Objects.requireNonNull(url);
        log.info("Fetching repo {} (this may take a while)", path);
        git.fetch()
                .setRemote(url.toString())
                .setRefSpecs(new RefSpec("+refs/*:refs/*"))
                .setTagOpt(TagOpt.NO_TAGS)
                .setForceUpdate(true)
                .setRemoveDeletedRefs(true)
                .setProgressMonitor(new RepoGitProgressMonitor("Fetching", path))
                .call();
        log.info("Finished fetching repo {}", path);
    }

    public void gc() throws GitAPIException {
        log.info("GCing repo {} (this may take a while)", path);
        git.gc()
                .setProgressMonitor(new RepoGitProgressMonitor("GCing", path))
                .setAggressive(false)
                .setPreserveOldPacks(false)
                .call();
        log.info("Finished GCing repo {}", path);
    }

    public void cloneTo(Path target, String hash) throws GitAPIException {
        log.info("Cloning repo {}", path);
        try (Git git = Git.init().setDirectory(target.toFile()).call()) {
            // Using the URL so we don't accidentally confuse directory and branch names.
            git.fetch()
                    .setRemote(path.toUri().toString())
                    .setRefSpecs(hash)
                    .setDepth(1)
                    .setProgressMonitor(new RepoGitProgressMonitor("Cloning", path))
                    .call();

            git.checkout().setName(hash).call();

            // No submodules for now
        }
        log.info("Finished cloning repo {}", path);
    }
}
