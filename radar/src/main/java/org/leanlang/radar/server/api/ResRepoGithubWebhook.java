package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.leanlang.radar.server.busser.Busser;
import org.leanlang.radar.server.busser.GhCommand;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;
import org.leanlang.radar.server.repos.github.JsonGhComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/repos/{repo}/github-webhook/")
public record ResRepoGithubWebhook(Repos repos, Busser busser) {

    public record JsonHookInfo(
            @JsonProperty(required = true) String action, @JsonProperty(required = true) JsonGhComment comment) {}

    private static final Logger log = LoggerFactory.getLogger(ResRepoGithubWebhook.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void get(
            @PathParam("repo") String repoName, @HeaderParam("X-GitHub-Event") String event, JsonHookInfo info) {

        log.debug("Got potential GitHub webhook for {}", repoName);

        Repo repo = repos.repo(repoName);
        if (repo.gh().isEmpty()) {
            log.debug("Webhook repo does not have a GitHub token");
            return;
        }

        if (!"issue_comment".equals(event)) {
            log.debug("Webhook type is not issue_comment");
            return;
        }

        if (!"created".equals(info.action)) {
            log.debug("Webhook is not for a newly created comment");
            return;
        }

        if (!GhCommand.isCommand(info.comment.body())) {
            log.debug("Webhook comment body is not a command");
            return;
        }

        log.info("Got GitHub issue comment creation webhook for {}", repoName);
        busser.updateRepo(repoName);
    }
}
