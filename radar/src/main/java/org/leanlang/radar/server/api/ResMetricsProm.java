package org.leanlang.radar.server.api;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.GITHUB_COMMAND_RUNNING;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.RUNS;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.queue.Task;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.Repos;

/**
 * Prometheus metrics.
 */
@Path("/metrics.prom")
public record ResMetricsProm(Repos repos, Queue queue) {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        StringBuilder sb = new StringBuilder();

        sb.append("# Queue\n");
        Map<String, List<Task>> tasksByRepo = queue.getTasks().stream()
                .collect(Collectors.groupingBy(it -> it.repo().name()));
        for (Repo repo : repos.repos()) {
            Integer tasks = Optional.ofNullable(tasksByRepo.get(repo.name()))
                    .map(List::size)
                    .orElse(0);
            addRepoMetric(sb, "queue_length", repo.name(), tasks);
        }

        sb.append("\n");
        sb.append("# History\n");
        for (Repo repo : repos.repos()) {
            int count = repo.db()
                    .read()
                    .dsl()
                    .selectCount()
                    .from(HISTORY)
                    .fetchOptional()
                    .map(Record1::value1)
                    .orElse(0);
            addRepoMetric(sb, "history_total", repo.name(), count);
        }

        sb.append("\n");
        sb.append("# Runs\n");
        for (Repo repo : repos.repos()) {
            int count = repo.db()
                    .read()
                    .dsl()
                    .selectCount()
                    .from(COMMITS)
                    .whereExists(DSL.selectOne().from(RUNS).where(RUNS.CHASH.eq(COMMITS.CHASH)))
                    .fetchOptional()
                    .map(Record1::value1)
                    .orElse(0);
            addRepoMetric(sb, "runs_total", repo.name(), count);
        }

        sb.append("\n");
        sb.append("# Github\n");
        for (Repo repo : repos.repos()) {
            int countAll = repo.db()
                    .read()
                    .dsl()
                    .selectCount()
                    .from(GITHUB_COMMAND_RUNNING)
                    .fetchOptional()
                    .map(Record1::value1)
                    .orElse(0);
            int countCompleted = repo.db()
                    .read()
                    .dsl()
                    .selectCount()
                    .from(GITHUB_COMMAND_RUNNING)
                    .where(GITHUB_COMMAND_RUNNING.COMPLETED_TIME.isNotNull())
                    .fetchOptional()
                    .map(Record1::value1)
                    .orElse(0);
            addRepoMetric(sb, "github_commands_total", repo.name(), countAll);
            addRepoMetric(sb, "github_commands_completed_total", repo.name(), countCompleted);
        }

        return sb.toString();
    }

    private static void addRepoMetric(StringBuilder sb, String topic, String repo, int value) {
        // https://prometheus.io/docs/practices/naming/
        sb.append("radar_")
                .append(topic)
                .append("{repo=\"")
                .append(repo)
                .append("\"} ")
                .append(value)
                .append("\n");
    }
}
