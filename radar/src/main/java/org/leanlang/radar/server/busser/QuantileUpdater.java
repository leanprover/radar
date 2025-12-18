package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.MEASUREMENTS;
import static org.leanlang.radar.codegen.jooq.Tables.METRICS;
import static org.leanlang.radar.codegen.jooq.Tables.QUANTILE;
import static org.leanlang.radar.codegen.jooq.Tables.QUANTILE_LAST_UPDATED;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jooq.Configuration;
import org.leanlang.radar.Constants;
import org.leanlang.radar.server.repos.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record QuantileUpdater(Repo repo) {
    private static final Logger log = LoggerFactory.getLogger(QuantileUpdater.class);

    public void update() throws GitAPIException {
        Instant now = Instant.now();
        if (!shouldUpdate(now)) return;
        log.info("Updating quantiles for repo {}", repo.name());

        repo.db().writeTransaction(ctx -> {
            List<String> metrics =
                    ctx.dsl().selectFrom(METRICS).orderBy(METRICS.METRIC).fetch(METRICS.METRIC);
            deleteQuantiles(ctx);

            Instant start = Instant.now();
            int n = metrics.size();
            int i = 0;

            for (String metric : metrics) {
                updateQuantileForMetric(ctx, metric);

                i++;
                if (i % 1000 != 0) continue;
                long msPerMetric = start.until(Instant.now()).toMillis() / i;
                log.debug("Quantile {}/{}, on average {} ms/metric", i, n, msPerMetric);
            }

            Duration total = start.until(Instant.now());
            log.debug("{} quantiles in {} s, on average {} ms/metric", n, total.toSeconds(), total.toMillis() / n);
        });

        setLastUpdated(now);
        log.info("Updated quantiles for repo {}", repo.name());
    }

    private Optional<Instant> lastUpdated() {
        return repo.db()
                .read()
                .dsl()
                .selectFrom(QUANTILE_LAST_UPDATED)
                .fetchOptional(QUANTILE_LAST_UPDATED.LAST_UPDATED_TIME);
    }

    private void setLastUpdated(Instant since) {
        log.info("Setting last updated time to {}", since);

        repo.db().writeTransaction(ctx -> {
            ctx.dsl().deleteFrom(QUANTILE_LAST_UPDATED).execute();
            ctx.dsl()
                    .insertInto(QUANTILE_LAST_UPDATED, QUANTILE_LAST_UPDATED.LAST_UPDATED_TIME)
                    .values(since)
                    .execute();
        });
    }

    private boolean shouldUpdate(Instant now) {
        Optional<Instant> lastUpdated = lastUpdated();
        if (lastUpdated.isEmpty()) return true;
        Duration timeSinceLastUpdate = lastUpdated.get().until(now);
        return timeSinceLastUpdate.compareTo(Constants.BUSSER_QUANTILE_LIFETIME) > 0;
    }

    private void deleteQuantiles(Configuration ctx) {
        ctx.dsl().deleteFrom(QUANTILE).execute();
    }

    private void updateQuantileForMetric(Configuration ctx, String metric) {
        List<Float> values = ctx.dsl()
                .selectFrom(HISTORY.join(MEASUREMENTS).on(MEASUREMENTS.CHASH.eq(HISTORY.CHASH)))
                .where(MEASUREMENTS.METRIC.eq(metric))
                .orderBy(HISTORY.POSITION.desc())
                .limit(400)
                .fetch(MEASUREMENTS.VALUE);

        List<Float> deltas = computeDeltas(values);
        Optional<Float> quantile = computeAbsQuantile(deltas, 0.9f);
        if (quantile.isEmpty()) return;

        ctx.dsl()
                .insertInto(QUANTILE, QUANTILE.METRIC, QUANTILE.VALUE)
                .values(metric, quantile.get())
                .execute();
    }

    private List<Float> computeDeltas(List<Float> values) {
        ArrayList<Float> deltas = new ArrayList<>();
        for (int i = 0; i < values.size() - 1; i++) {
            Float first = values.get(i);
            Float second = values.get(i + 1);
            if (first == null || second == null) continue;
            deltas.add(second - first);
        }
        return deltas;
    }

    private Optional<Float> computeAbsQuantile(List<Float> deltas, float quantile) {
        if (deltas.size() < 10) return Optional.empty();
        quantile = Float.max(0, Float.min(1, quantile));

        List<Float> values = deltas.stream().map(Math::abs).sorted().toList();
        int top = values.size() - 1;

        float x = quantile * top;
        int index = Math.round(x);
        if (index >= top) return Optional.of(values.get(top));

        float weight = x - index;
        float result = values.get(index) * (1 - weight) + values.get(index + 1) * weight;
        return Optional.of(result);
    }
}
