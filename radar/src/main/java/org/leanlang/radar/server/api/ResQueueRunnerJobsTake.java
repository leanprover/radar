package org.leanlang.radar.server.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import org.leanlang.radar.server.queue.Job;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.runners.Runner;
import org.leanlang.radar.server.runners.Runners;

@Path(ResQueueRunnerJobsTake.PATH)
public record ResQueueRunnerJobsTake(Runners runners, Queue queue) {
    public static final String PATH = "/queue/runner/jobs/take";

    public record JsonPostInput(@NotNull String runner, @NotNull String token) {}

    public record JsonJob(String repo, URI url, String chash, URI benchUrl, String benchChash, String script) {
        public JsonJob(Job job) {
            this(job.repo(), job.url(), job.chash(), job.benchUrl(), job.benchChash(), job.script());
        }

        public Job toJob() {
            return new Job(repo, url, chash, benchUrl, benchChash, script);
        }
    }

    public record JsonPost(Optional<JsonJob> job) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonPost post(JsonPostInput input) throws IOException {
        Runner runner = runners.runner(input.runner, input.token);
        Optional<JsonJob> job = queue.takeJob(runner.name()).map(JsonJob::new);
        return new JsonPost(job);
    }
}
