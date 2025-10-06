package org.leanlang.radar.server.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.leanlang.radar.server.busser.Busser;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/repos/{repo}/github-webhook/")
public record ResRepoGithubWebhook(Repos repos, Busser busser) {

    private static final Logger log = LoggerFactory.getLogger(ResRepoGithubWebhook.class);

    @POST
    public void get(@PathParam("repo") String repoName) {
        Repo repo = repos.repo(repoName);
        log.info("Got GitHub webhook for {}", repo.name());

        if (repo.gh().isEmpty())
            throw new RuntimeException(
                    "Attempting to execute GitHub webhook for repo " + repoName + " with no GitHub token");

        busser.updateRepo(repoName);
    }
}
