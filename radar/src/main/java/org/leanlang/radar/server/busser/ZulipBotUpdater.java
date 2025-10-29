package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.SIGNIFICANCE_FEED;
import static org.leanlang.radar.codegen.jooq.Tables.ZULIP_FEED;

import java.util.List;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.Formatter;
import org.leanlang.radar.codegen.jooq.tables.History;
import org.leanlang.radar.server.compare.CommitComparer;
import org.leanlang.radar.server.compare.JsonMessageSegment;
import org.leanlang.radar.server.compare.JsonMetricComparison;
import org.leanlang.radar.server.compare.JsonMetricSignificance;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoZulip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ZulipBotUpdater(Repo repo, RepoZulip repoZulip, String channel, String topic) {
    private static final Logger log = LoggerFactory.getLogger(ZulipBotUpdater.class);
    private static final String RADAR_URL = "https://radar.lean-lang.org/"; // TODO Configurable via config file

    public void update() {
        log.info("Updating Zulip bot for repo {}", repo.name());
        markAllCommitsSeenOnInitialRun();
        sendMessagesForSignificantCommits();
        log.info("Updated Zulip bot for repo {}", repo.name());
    }

    private void markAllCommitsSeenOnInitialRun() {
        repo.db().writeTransaction(ctx -> {
            boolean anySeen =
                    ctx.dsl().selectOne().from(ZULIP_FEED).limit(1).fetch().isNotEmpty();
            if (anySeen) return;

            int added = ctx.dsl()
                    .insertInto(ZULIP_FEED, ZULIP_FEED.CHASH)
                    .select(DSL.select(HISTORY.CHASH).from(HISTORY))
                    .execute();

            if (added > 0) {
                log.info("Marked {} commits as seen initially", added);
            }
        });
    }

    private void sendMessagesForSignificantCommits() {
        History parent = HISTORY.as("parent");
        History child = HISTORY.as("child");

        Result<Record3<String, String, String>> rows = repo.db()
                .read()
                .dsl()
                .select(parent.CHASH, child.CHASH, COMMITS.MESSAGE_TITLE)
                .from(child.leftJoin(parent)
                        .on(parent.POSITION.add(1).eq(child.POSITION))
                        .join(COMMITS)
                        .on(COMMITS.CHASH.eq(child.CHASH)))
                .whereExists(DSL.selectOne().from(SIGNIFICANCE_FEED).where(SIGNIFICANCE_FEED.CHASH.eq(child.CHASH)))
                .andNotExists(DSL.selectOne().from(ZULIP_FEED).where(ZULIP_FEED.CHASH.eq(child.CHASH)))
                .orderBy(child.POSITION.asc())
                .fetch();

        for (Record3<String, String, String> row : rows) {
            String parentChash = row.value1(); // May be null
            String childChash = row.value2();
            String childTitle = row.value3();
            sendMessageForCommit(parentChash, childChash, childTitle);
        }
    }

    private void sendMessageForCommit(@Nullable String parentChash, String childChash, String childTitle) {
        List<JsonMetricComparison> comparisons = CommitComparer.compareCommits(repo, parentChash, childChash);
        boolean significant =
                comparisons.stream().anyMatch(it -> it.significance().isPresent());

        if (significant) {
            String content = messageContent(parentChash, childChash, childTitle, comparisons);
            repoZulip.sendMessage(channel, topic, content);
            log.info("Sent message for significant commit {}", childChash);
        } else {
            log.info("Sent no message for insignificant commit {}", childChash);
        }

        repo.db().writeTransaction(ctx -> ctx.dsl()
                .insertInto(ZULIP_FEED, ZULIP_FEED.CHASH)
                .values(childChash)
                .onDuplicateKeyIgnore()
                .execute());
    }

    /*
     * Messages
     */

    private String radarLinkToCommit(String chash) {
        return RADAR_URL + "repos/" + repo.name() + "/commits/" + chash;
    }

    private String messageContent(
            @Nullable String parentChash,
            String childChash,
            String childTitle,
            List<JsonMetricComparison> comparisons) {
        StringBuilder sb = new StringBuilder();

        sb.append("**[")
                .append(childTitle)
                .append("](")
                .append(radarLinkToCommit(childChash))
                .append(")**\n");

        List<List<JsonMessageSegment>> major = comparisons.stream()
                .flatMap(it -> it.significance().stream())
                .filter(JsonMetricSignificance::major)
                .map(JsonMetricSignificance::message)
                .toList();
        List<List<JsonMessageSegment>> minor = comparisons.stream()
                .flatMap(it -> it.significance().stream())
                .filter(it -> !it.major())
                .map(JsonMetricSignificance::message)
                .toList();

        sb.append("\n");
        formatSignificanceSection(sb, "Major changes", major);
        sb.append("\n");
        formatSignificanceSection(sb, "Minor changes", minor);

        return sb.toString();
    }

    private void formatSignificanceSection(StringBuilder sb, String name, List<List<JsonMessageSegment>> messages) {
        if (messages.isEmpty()) return;

        sb.append("**").append(name).append("** (").append(messages.size()).append(")\n\n");

        for (List<JsonMessageSegment> message : messages) {
            sb.append("- ");
            formatMessage(sb, message);
            sb.append("\n");
        }
    }

    private void formatMessage(StringBuilder sb, List<JsonMessageSegment> message) {
        for (JsonMessageSegment segment : message) {
            formatMessageSegment(sb, segment);
        }
    }

    private void formatMessageSegment(StringBuilder sb, JsonMessageSegment segment) {
        Formatter fmt = new Formatter().withSign(true);
        switch (segment) {
            case JsonMessageSegment.Delta it:
                sb.append("**")
                        .append(fmt.formatValueWithUnit(it.amount(), it.unit().orElse(null)))
                        .append("**");
                if (it.amount() * it.direction() > 0) sb.append(" (✅)");
                else if (it.amount() * it.direction() < 0) sb.append(" (\uD83D\uDFE5)");
                break;
            case JsonMessageSegment.DeltaPercent it:
                sb.append("**").append(fmt.formatValue(it.factor(), "100%")).append("**");
                if (it.factor() * it.direction() > 0) sb.append(" (✅)");
                else if (it.factor() * it.direction() < 0) sb.append(" (\uD83D\uDFE5)");
                break;
            case JsonMessageSegment.Metric it:
                sb.append("`").append(it.metric()).append("`");
                break;
            case JsonMessageSegment.Text it:
                sb.append(it.text());
                break;
        }
    }
}
