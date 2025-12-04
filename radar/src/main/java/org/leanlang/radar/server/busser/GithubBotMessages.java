package org.leanlang.radar.server.busser;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.compare.JsonMessage;
import org.leanlang.radar.server.compare.JsonMessageGoodness;
import org.leanlang.radar.server.compare.JsonMessageSegment;
import org.leanlang.radar.server.compare.JsonSignificance;
import org.leanlang.radar.util.Formatter;
import org.leanlang.radar.util.GithubLinker;
import org.leanlang.radar.util.RadarLinker;

public record GithubBotMessages(RadarLinker radarLinker, GithubLinker githubLinker) {
    private static final String EDIT_POSSIBLE =
            "\n\n<sub>You can edit the original message until the command succeeds.</sub>";

    public String notInPr() {
        return "This command can only be used in pull requests.";
    }

    public String deleted() {
        return "The original message has been deleted.";
    }

    public String noLongerACommand() {
        return "The original message no longer contains a command." + EDIT_POSSIBLE;
    }

    public String tooManyCommands() {
        return "The original message contains multiple commands. Please only use one command at a time."
                + EDIT_POSSIBLE;
    }

    public String blockedByLabels(List<String> blockingLabels) {
        String labelLinks = blockingLabels.stream()
                .map(githubLinker::label)
                .map(URI::toString)
                .collect(Collectors.joining(", "));

        if (blockingLabels.size() == 1) return "Waiting until the label " + labelLinks + " is removed." + EDIT_POSSIBLE;
        else return "Waiting until the labels " + labelLinks + " are removed." + EDIT_POSSIBLE;
    }

    public String failedToFindMergeBase() {
        return "Failed to find a commit to compare against." + EDIT_POSSIBLE;
    }

    public String benchMathlibNotImplemented() {
        return "The `!bench mathlib` command is not yet implemented." + EDIT_POSSIBLE;
    }

    public String inProgress(String repo, String chashFirst, String chashSecond) {
        return "Benchmarking "
                + chashSecond + " ([status](" + radarLinker.commit(repo, chashSecond) + "))"
                + " against "
                + chashFirst + " ([status](" + radarLinker.commit(repo, chashFirst) + "))"
                + ".\n\n"
                + "<sub>React with :eyes: to be notified when the results are in."
                + " The command author is always notified.</sub>";
    }

    public String finished(
            String repo,
            String chashFirst,
            String chashSecond,
            String userLogin,
            List<String> usersThatReactedWithEye,
            JsonCommitComparison comparison) {

        StringBuilder sb = new StringBuilder();

        sb.append("[Benchmark results](")
                .append(radarLinker.comparison(repo, chashFirst, chashSecond))
                .append(") for ")
                .append(chashSecond)
                .append(" against ")
                .append(chashFirst)
                .append(" are in!");

        Stream.concat(Stream.of(userLogin), usersThatReactedWithEye.stream()).collect(Collectors.toSet()).stream()
                .sorted()
                .forEach(it -> sb.append(" @").append(it));

        List<JsonMessage> significantRuns = getSignificantRuns(comparison);
        List<JsonMessage> significantMajorMetrics = getSignificantMajorMetrics(comparison);
        List<JsonMessage> significantMinorMetrics = getSignificantMinorMetrics(comparison);

        formatSignificanceSection(sb, "Runs", significantRuns);
        formatSignificanceSection(sb, "Major changes", significantMajorMetrics);
        formatSignificanceSection(sb, "Minor changes", significantMinorMetrics);

        return sb.toString();
    }

    private void formatSignificanceSection(StringBuilder sb, String name, List<JsonMessage> messages) {
        if (messages.isEmpty()) return;

        sb.append("\n<details open>\n");

        sb.append("<summary>")
                .append(name)
                .append(" (")
                .append(messages.size())
                .append(")")
                .append("</summary>\n");

        // If there's no empty line between the <summary> and the list, GitHub won't render it correctly.
        sb.append("\n");

        for (JsonMessage message : messages) {
            sb.append("- ");
            formatMessage(sb, message);
            sb.append("\n");
        }

        sb.append("</details>");
    }

    public static List<JsonMessage> getSignificantRuns(JsonCommitComparison comparison) {
        return comparison.runSignificances().map(JsonSignificance::message).toList();
    }

    public static List<JsonMessage> getSignificantMajorMetrics(JsonCommitComparison comparison) {
        return comparison
                .metricSignificances()
                .filter(JsonSignificance::major)
                .map(JsonSignificance::message)
                .toList();
    }

    public static List<JsonMessage> getSignificantMinorMetrics(JsonCommitComparison comparison) {
        return comparison
                .metricSignificances()
                .filter(it -> !it.major())
                .map(JsonSignificance::message)
                .toList();
    }

    public static void formatMessage(StringBuilder sb, JsonMessage message) {
        formatMessageGoodness(sb, message.goodness(), true);
        for (JsonMessageSegment segment : message.segments()) {
            formatMessageSegment(sb, segment);
        }
    }

    private static void formatMessageGoodness(StringBuilder sb, JsonMessageGoodness goodness, boolean trailingSpace) {
        switch (goodness) {
            case JsonMessageGoodness.GOOD -> {
                sb.append("✅");
                if (trailingSpace) sb.append(" ");
            }
            case JsonMessageGoodness.BAD -> {
                sb.append("\uD83D\uDFE5");
                if (trailingSpace) sb.append(" ");
            }
            case JsonMessageGoodness.NEUTRAL -> {}
        }
    }

    public static void formatMessageSegment(StringBuilder sb, JsonMessageSegment segment) {
        Formatter fmt = new Formatter().withSign(true);
        switch (segment) {
            case JsonMessageSegment.Delta it:
                sb.append("**")
                        .append(fmt.formatValueWithUnit(it.amount(), it.unit().orElse(null)))
                        .append("**");
                break;
            case JsonMessageSegment.DeltaPercent it:
                sb.append("**").append(fmt.formatValue(it.factor(), "100%")).append("**");
                break;
            case JsonMessageSegment.ExitCode it:
                sb.append("**").append(it.exitCode()).append("**");
                break;
            case JsonMessageSegment.Metric it:
                sb.append("`").append(it.metric()).append("`");
                break;
            case JsonMessageSegment.Run it:
                sb.append("`").append(it.run()).append("`");
                break;
            case JsonMessageSegment.Text it:
                sb.append(it.text());
                break;
        }
    }
}
