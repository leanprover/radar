package org.leanlang.radar.server.busser;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.Constants;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.compare.JsonMessage;
import org.leanlang.radar.server.compare.JsonMessageGoodness;
import org.leanlang.radar.server.compare.JsonMessageSegment;
import org.leanlang.radar.util.Formatter;
import org.leanlang.radar.util.RadarLinker;

public record BotMsgBuilder(RadarLinker radarLinker, String repo, String chash) {
    public static final String EMOJI_GOOD = "✅";
    public static final String EMOJI_BAD = "\uD83D\uDFE5";

    public void formatBody(StringBuilder sb, JsonCommitComparison comparison) {
        formatMessageSection(sb, null, comparison.notes());
        formatMessageSection(sb, "New metrics", comparison.newMetrics());
        formatMessageSection(sb, "Large changes", comparison.largeChanges());
        formatMessageSection(sb, "Medium changes", comparison.mediumChanges());
        formatMessageSection(sb, "Small changes", comparison.smallChanges());

        if (comparison.largeChanges().isEmpty()
                && comparison.mediumChanges().isEmpty()
                && comparison.smallChanges().isEmpty()) {
            sb.append("\n\nNo significant changes detected.");
        }
    }

    private void formatMessageSection(StringBuilder sb, @Nullable String title, List<JsonMessage> messages) {
        if (messages.isEmpty()) return;

        // Heading
        if (title != null && !title.isBlank()) {
            sb.append("\n\n**").append(title).append(" (");
            formatMessageCounters(sb, messages);
            sb.append(")**");
        }

        // List
        List<JsonMessage> visible = messages.stream().filter(it -> !it.hidden()).toList();
        int hidden = messages.size() - visible.size();

        List<JsonMessage> shown = visible.stream()
                .limit(Constants.BOT_MESSAGE_MAX_VISIBLE_ENTRIES)
                .toList();
        int more = visible.size() - shown.size();

        sb.append("\n");
        for (JsonMessage message : shown) {
            sb.append("\n- ");
            formatMessage(sb, message);
        }
        if (more > 0) {
            sb.append("\n- *and ").append(more).append(" more*");
        }
        if (hidden > 0) {
            sb.append("\n- *")
                    .append(shown.isEmpty() ? "" : "and ")
                    .append(hidden)
                    .append(" hidden*");
        }
    }

    private static void formatGoodness(StringBuilder sb, JsonMessageGoodness goodness, boolean trailingSpace) {
        switch (goodness) {
            case BAD -> {
                sb.append(EMOJI_BAD);
                if (trailingSpace) sb.append(" ");
            }
            case GOOD -> {
                sb.append(EMOJI_GOOD);
                if (trailingSpace) sb.append(" ");
            }
            case NEUTRAL -> {}
        }
    }

    private static void formatMessageCounters(StringBuilder sb, List<JsonMessage> messages) {
        long good = messages.stream()
                .filter(it -> it.goodness() == JsonMessageGoodness.GOOD)
                .count();
        long bad = messages.stream()
                .filter(it -> it.goodness() == JsonMessageGoodness.BAD)
                .count();
        long neutral = messages.size() - good - bad;

        boolean atLeastOneElement = false;
        if (good > 0) {
            sb.append(good);
            formatGoodness(sb, JsonMessageGoodness.GOOD, false);
            atLeastOneElement = true;
        }
        if (bad > 0) {
            if (atLeastOneElement) sb.append(", ");
            sb.append(bad);
            formatGoodness(sb, JsonMessageGoodness.BAD, false);
            atLeastOneElement = true;
        }
        if (neutral > 0) {
            if (atLeastOneElement) sb.append(", ");
            sb.append(neutral);
        }
    }

    private void formatMessage(StringBuilder sb, JsonMessage message) {
        formatGoodness(sb, message.goodness(), true);
        for (JsonMessageSegment segment : message.segments()) {
            formatMessageSegment(sb, segment);
        }
    }

    private void formatMessageSegment(StringBuilder sb, JsonMessageSegment segment) {
        Formatter fmt = new Formatter().withSign(true);
        switch (segment) {
            case JsonMessageSegment.Delta it ->
                sb.append("**")
                        .append(fmt.formatValueWithUnit(it.amount(), it.unit().orElse(null)))
                        .append("**");
            case JsonMessageSegment.DeltaPercent it ->
                sb.append("**")
                        .append(fmt.withPrecision(2).formatValue(it.factor(), "100%"))
                        .append("**");
            case JsonMessageSegment.ExitCode it ->
                sb.append("**").append(it.exitCode()).append("**");
            case JsonMessageSegment.Metric it ->
                sb.append("`").append(it.metric()).append("`");
            case JsonMessageSegment.Run it ->
                sb.append("[`")
                        .append(it.run())
                        .append("`](")
                        .append(radarLinker.run(repo, chash, it.run()))
                        .append(")");
            case JsonMessageSegment.Text it -> sb.append(it.text());
        }
    }
}
