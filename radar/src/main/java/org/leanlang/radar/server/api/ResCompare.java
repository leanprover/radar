package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Configuration;
import org.leanlang.radar.codegen.jooq.Tables;
import org.leanlang.radar.codegen.jooq.tables.History;
import org.leanlang.radar.codegen.jooq.tables.records.MeasurementsRecord;
import org.leanlang.radar.server.data.Repos;

@Path("/compare/{repo}/{first}/{second}/")
public record ResCompare(Repos repos) {
    public record JsonMeasurement(
            String metric, Optional<Float> first, Optional<Float> second, Optional<String> unit, int direction) {}

    public record JsonGet(
            Optional<String> chashFirst, Optional<String> chashSecond, List<JsonMeasurement> measurements) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonGet get(
            @PathParam("repo") String name, @PathParam("first") String first, @PathParam("second") String second) {

        return repos.repo(name).db().readTransactionResult(ctx -> {
            Optional<String> chashFirst = resolveRelativeTo(ctx, first, second);
            Optional<String> chashSecond = resolveRelativeTo(ctx, second, first);

            Map<String, Float> measurementsFirst =
                    chashFirst.map(it -> measurementsFor(ctx, it)).orElse(Map.of());
            Map<String, Float> measurementsSecond =
                    chashSecond.map(it -> measurementsFor(ctx, it)).orElse(Map.of());

            List<JsonMeasurement> measurements =
                    ctx.dsl().selectFrom(Tables.METRICS).orderBy(Tables.METRICS.METRIC.asc()).stream()
                            .map(metric -> new JsonMeasurement(
                                    metric.getMetric(),
                                    Optional.ofNullable(measurementsFirst.get(metric.getMetric())),
                                    Optional.ofNullable(measurementsSecond.get(metric.getMetric())),
                                    Optional.ofNullable(metric.getUnit()),
                                    metric.getDirection()))
                            .filter(it -> it.first.isPresent() || it.second.isPresent())
                            .toList();

            return new JsonGet(chashFirst, chashSecond, measurements);
        });
    }

    private Optional<String> resolveRelativeTo(Configuration ctx, String chash, String base) {
        History h1 = HISTORY.as("h1");
        History h2 = HISTORY.as("h2");

        if (chash.equals("parent")) {
            String result = ctx.dsl()
                    .selectFrom(h1.join(h2).on(h1.POSITION.add(1).eq(h2.POSITION)))
                    .where(h2.CHASH.eq(base))
                    .fetchOne(h1.CHASH);
            return Optional.ofNullable(result);
        }

        if (chash.equals("child")) {
            String result = ctx.dsl()
                    .selectFrom(h1.join(h2).on(h1.POSITION.add(1).eq(h2.POSITION)))
                    .where(h1.CHASH.eq(base))
                    .fetchOne(h2.CHASH);
            return Optional.ofNullable(result);
        }

        return Optional.of(chash);
    }

    private Map<String, Float> measurementsFor(Configuration ctx, String chash) {
        return ctx.dsl().selectFrom(MEASUREMENTS).where(MEASUREMENTS.CHASH.eq(chash)).stream()
                .collect(Collectors.toMap(MeasurementsRecord::getMetric, MeasurementsRecord::getValue));
    }
}
