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
import org.leanlang.radar.codegen.jooq.tables.History;
import org.leanlang.radar.server.compare.CommitComparer;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.compare.JsonMessageSegment;
import org.leanlang.radar.server.compare.JsonSignificance;
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
        JsonCommitComparison comparison = CommitComparer.compareCommits(repo, parentChash, childChash);

        if (comparison.significant()) {
            String content = messageContent(childChash, childTitle, comparison);
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

    private String messageContent(String childChash, String childTitle, JsonCommitComparison comparison) {
        StringBuilder sb = new StringBuilder();

        sb.append("**[")
                .append(childTitle)
                .append("](")
                .append(radarLinkToCommit(childChash))
                .append(")**\n");

        List<List<JsonMessageSegment>> significantRuns =
                comparison.runSignificances().map(JsonSignificance::message).toList();
        List<List<JsonMessageSegment>> significantMajorMetrics = comparison
                .metricSignificances()
                .filter(JsonSignificance::major)
                .map(JsonSignificance::message)
                .toList();
        List<List<JsonMessageSegment>> significantMinorMetrics = comparison
                .metricSignificances()
                .filter(it -> !it.major())
                .map(JsonSignificance::message)
                .toList();

        formatSignificanceSection(sb, "Runs", significantRuns);
        formatSignificanceSection(sb, "Major changes", significantMajorMetrics);
        formatSignificanceSection(sb, "Minor changes", significantMinorMetrics);

        return sb.toString();
    }

    private void formatSignificanceSection(StringBuilder sb, String name, List<List<JsonMessageSegment>> messages) {
        if (messages.isEmpty()) return;
        sb.append("\n");

        sb.append("**").append(name).append("** (").append(messages.size()).append(")\n\n");

        for (List<JsonMessageSegment> message : messages) {
            sb.append("- ");
            formatMessage(sb, message);
            sb.append("\n");
        }
    }

    private void formatMessage(StringBuilder sb, List<JsonMessageSegment> message) {
        for (JsonMessageSegment segment : message) {
            GithubBotUpdater.formatMessageSegment(sb, segment);
        }
    }
}
