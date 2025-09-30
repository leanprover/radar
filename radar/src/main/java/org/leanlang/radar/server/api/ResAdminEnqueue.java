package org.leanlang.radar.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.leanlang.radar.Constants;
import org.leanlang.radar.server.api.auth.Admin;
import org.leanlang.radar.server.data.Repo;
import org.leanlang.radar.server.data.Repos;
import org.leanlang.radar.server.queue.Queue;

@Path("/admin/enqueue/")
public record ResAdminEnqueue(Repos repos, Queue queue) {

    public record JsonPostInput(
            @JsonProperty(required = true) String repo,
            @JsonProperty(required = true) String chash,
            Optional<Integer> priority) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(@Auth Admin admin, JsonPostInput input) {
        Repo repo = repos.repo(input.repo);
        queue.enqueueHard(repo.name(), input.chash, input.priority.orElse(Constants.PRIORITY_NEW_COMMIT));
    }
}
