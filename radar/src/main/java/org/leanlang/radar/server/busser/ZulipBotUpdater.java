package org.leanlang.radar.server.busser;

import static org.leanlang.radar.codegen.jooq.Tables.COMMITS;
import static org.leanlang.radar.codegen.jooq.Tables.HISTORY;
import static org.leanlang.radar.codegen.jooq.Tables.SIGNIFICANCE_FEED;
import static org.leanlang.radar.codegen.jooq.Tables.ZULIP_FEED;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.codegen.jooq.tables.History;
import org.leanlang.radar.server.compare.CommitComparer;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.queue.Queue;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.RepoZulip;
import org.leanlang.radar.util.RadarLinker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ZulipBotUpdater(
        Queue queue,
        RadarLinker radarLinker,
        Repo repo,
        RepoZulip repoZulip,
        String channel,
        String topic,
        Optional<String> linkifier) {

    private static final Logger log = LoggerFactory.getLogger(ZulipBotUpdater.class);

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
        JsonCommitComparison comparison = CommitComparer.compareCommits(queue, repo, parentChash, childChash);

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

    private String messageContent(String childChash, String childTitle, JsonCommitComparison comparison) {
        StringBuilder sb = new StringBuilder();

        sb.append("**");
        formatTitle(sb, childChash, childTitle);
        sb.append("**");

        GithubBotMessages.formatBody(sb, comparison);

        return sb.toString();
    }

    private void formatTitle(StringBuilder sb, String chash, String title) {
        URI url = radarLinker.commit(repo.name(), chash);
        if (linkifier.isEmpty()) {
            sb.append("[").append(title).append("](").append(url).append(")");
            return;
        }

        Matcher m = Pattern.compile("(?<!\\w)(#\\d+)\\b").matcher(title);
        int end = 0;

        while (m.find()) {
            int mStart = m.start(1);
            int mEnd = m.end(1);

            if (mStart > end)
                sb.append("[")
                        .append(title, end, mStart)
                        .append("](")
                        .append(url)
                        .append(")");

            sb.append(linkifier.get()).append(m.group(1));
            end = mEnd;
        }

        if (end < title.length())
            sb.append("[")
                    .append(title, end, title.length())
                    .append("](")
                    .append(url)
                    .append(")");
    }
}
