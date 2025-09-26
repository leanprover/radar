package org.leanlang.radar.server.api;

import io.dropwizard.auth.Auth;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.leanlang.radar.server.api.auth.Admin;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.queue.Queue;

@Path("/admin/enqueue/")
public record ResAdminEnqueue(Repos repos, Queue queue) {

    public record JsonPostInput(@NotNull String repo, @NotNull String chash, @NotNull Optional<Integer> priority) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(@Auth Admin admin, JsonPostInput input) {
        Repo repo = repos.repo(input.repo);
        queue.enqueueHard(repo.name(), input.chash, input.priority.orElse(0));
    }
}
