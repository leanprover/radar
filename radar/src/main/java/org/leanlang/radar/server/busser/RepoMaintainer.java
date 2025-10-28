package org.leanlang.radar.server.busser;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.leanlang.radar.server.repos.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record RepoMaintainer(Repo repo) {
    private static final Logger log = LoggerFactory.getLogger(RepoMaintainer.class);

    public void maintain(boolean aggressive) {
        log.info("Cleaning repo {}", repo.name());
        if (aggressive) dbVacuum();
        dbPragmaOptimize();
        gitGc();
        log.info("Cleaned repo {}", repo.name());
    }

    private void dbVacuum() {
        log.info("Vacuuming");
        try {
            repo.db().writeWithoutTransactionDoNotUseUnlessYouKnowWhatYouAreDoing(ctx -> ctx.dsl()
                    .execute("VACUUM"));
        } catch (Throwable e) {
            log.error("Failed to vacuum", e);
        }
        log.info("Vacuumed");
    }

    private void dbPragmaOptimize() {
        // https://sqlite.org/lang_analyze.html#periodically_run_pragma_optimize_
        log.info("Running pragma optimize");
        repo.db().writeTransaction(ctx -> ctx.dsl().execute("PRAGMA optimize"));
        log.info("Ran pragma optimize");
    }

    private void gitGc() {
        log.info("GC git repos");
        try {
            repo.git().gc();
            repo.gitBench().gc();
        } catch (GitAPIException e) {
            log.error("Failed to GC git repos", e);
        }
        log.info("GCed git repos");
    }
}
