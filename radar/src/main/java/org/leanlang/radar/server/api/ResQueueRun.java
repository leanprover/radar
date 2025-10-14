package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.leanlang.radar.runner.supervisor.JsonOutputLineBatch;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.queue.Run;
import org.leanlang.radar.server.queue.Task;

@Path("/queue/runs/{repo}/{chash}/{run}/")
public record ResQueueRun(Queue queue) {

    public record JsonActiveRun(
            @JsonProperty(required = true) String benchChash,
            @JsonProperty(required = true) Instant startTime,
            @JsonProperty(required = true) JsonOutputLineBatch lines) {}

    public record JsonGet(
            @JsonProperty(required = true) String runner,
            @JsonProperty(required = true) String script,
            Optional<JsonActiveRun> activeRun) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(
            @PathParam("repo") String repoName, @PathParam("chash") String chash, @PathParam("run") String runName)
            throws IOException {

        Task task = queue.getTask(repoName, chash).orElseThrow(NotFoundException::new);
        Run run = task.runs().stream()
                .filter(it -> it.name().equals(runName))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        return new JsonGet(
                run.runner(),
                run.script(),
                run.active().map(it -> new JsonActiveRun(it.benchChash(), it.startTime(), it.lines())));
    }
}
