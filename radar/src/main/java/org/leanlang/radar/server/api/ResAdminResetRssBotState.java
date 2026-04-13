package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.ZULIP_FEED;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.auth.Auth;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.leanlang.radar.server.api.auth.Admin;
import org.leanlang.radar.server.repos.Repos;

@Path("/admin/reset-rss-bot-state/")
public record ResAdminResetRssBotState(Repos repos) {

    public record JsonPostInput(
            @JsonProperty(required = true) String repo,
            @JsonProperty(required = true) String chash) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(@Auth Admin admin, JsonPostInput input) {
        repos.repo(input.repo).db().writeTransaction(ctx -> ctx.dsl()
                .deleteFrom(ZULIP_FEED)
                .where(ZULIP_FEED.CHASH.eq(input.chash))
                .execute());
    }
}
