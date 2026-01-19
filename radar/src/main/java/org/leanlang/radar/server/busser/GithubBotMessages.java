package org.leanlang.radar.server.busser;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.compare.JsonMessage;
import org.leanlang.radar.server.compare.JsonMessageGoodness;
import org.leanlang.radar.server.compare.JsonMessageSegment;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.source.RepoSourceGithub;
import org.leanlang.radar.util.Formatter;
import org.leanlang.radar.util.GithubLinker;
import org.leanlang.radar.util.RadarLinker;

public record GithubBotMessages(RadarLinker radarLinker, GithubLinker githubLinker) {
    private static final String EDIT_POSSIBLE =
            "\n\n<sub>You can edit the original message until the command succeeds.</sub>";

    public static final String WARNINGS_EXPLANATION =
            "These warnings may indicate that the benchmark results are not directly comparable,"
                    + " for example due to changes in the runner configuration or hardware.";

    public static final String EMOJI_GOOD = "✅";
    public static final String EMOJI_BAD = "\uD83D\uDFE5";

    public String msgNotInPr() {
        return "This command can only be used in pull requests.";
    }

    public String msgDeleted() {
        return "The original message has been deleted.";
    }

    public String msgNoLongerACommand() {
        return "The original message no longer contains a command." + EDIT_POSSIBLE;
    }

    public String msgTooManyCommands() {
        return "The original message contains multiple commands. Please only use one command at a time."
                + EDIT_POSSIBLE;
    }

    public String msgRepoIsNotMathlib() {
        return "This command can only be used in the mathlib repository." + EDIT_POSSIBLE;
    }

    public String msgLabelMismatch(List<String> superfluousLabels, List<String> missingLabels) {
        StringBuilder sb = new StringBuilder();

        sb.append("Waiting until ");

        if (!superfluousLabels.isEmpty()) {
            String links = superfluousLabels.stream()
                    .map(githubLinker::label)
                    .map(URI::toString)
                    .collect(Collectors.joining(" "));

            if (superfluousLabels.size() == 1)
                sb.append("the label ").append(links).append(" is removed");
            else sb.append("the labels ").append(links).append(" are removed");
        }

        if (!superfluousLabels.isEmpty() && !missingLabels.isEmpty()) {
            sb.append(" and ");
        }

        if (!missingLabels.isEmpty()) {
            String links = missingLabels.stream()
                    .map(githubLinker::label)
                    .map(URI::toString)
                    .collect(Collectors.joining(" "));

            if (missingLabels.size() == 1) sb.append("the label ").append(links).append(" is added");
            else sb.append("the labels ").append(links).append(" are added");
        }

        sb.append(".").append(EDIT_POSSIBLE);
        return sb.toString();
    }

    public String msgFailedToFindMergeBase() {
        return "Failed to find a commit to compare against." + EDIT_POSSIBLE;
    }

    private String linkToChash(@Nullable Repo repo, String chash) {
        if (repo == null) return chash;
        if (repo.source() instanceof RepoSourceGithub(String ghOwner, String ghRepo))
            return new GithubLinker(ghOwner, ghRepo).commit(chash).toString();
        return chash;
    }

    public String msgInProgress(Repo repo, boolean repoForeign, String chashFirst, String chashSecond) {
        return "Benchmarking "
                + linkToChash(repoForeign ? repo : null, chashSecond)
                + " against "
                + linkToChash(repoForeign ? repo : null, chashFirst)
                + " ([preliminary results](" + radarLinker.comparison(repo.name(), chashFirst, chashSecond) + "))"
                + ".\n\n"
                + "<sub>React with :eyes: to be notified when the results are in."
                + " The command author is always notified.</sub>";
    }

    public String msgFinished(
            Repo repo,
            boolean repoForeign,
            String chashFirst,
            String chashSecond,
            String userLogin,
            List<String> usersThatReactedWithEye,
            JsonCommitComparison comparison) {

        StringBuilder sb = new StringBuilder();

        sb.append("[Benchmark results](")
                .append(radarLinker.comparison(repo.name(), chashFirst, chashSecond))
                .append(") for ")
                .append(linkToChash(repoForeign ? repo : null, chashSecond))
                .append(" against ")
                .append(linkToChash(repoForeign ? repo : null, chashFirst))
                .append(" are in!");

        Stream.concat(Stream.of(userLogin), usersThatReactedWithEye.stream()).collect(Collectors.toSet()).stream()
                .sorted()
                .forEach(it -> sb.append(" @").append(it));

        formatWarnings(sb, comparison);
        formatBody(sb, comparison);

        return sb.toString();
    }

    public static void formatWarnings(StringBuilder sb, JsonCommitComparison comparison) {
        if (comparison.warnings().isEmpty()) return;

        sb.append("\n")
                .append("\n> [!WARNING]")
                .append("\n> " + WARNINGS_EXPLANATION)
                .append("\n>");

        for (String warning : comparison.warnings()) sb.append("\n> - ").append(warning);
    }

    public static void formatBody(StringBuilder sb, JsonCommitComparison comparison) {
        formatMessageSection(sb, null, comparison.notes());
        formatMessageSection(sb, "Large changes", comparison.largeChanges());
        formatMessageSection(sb, "Medium changes", comparison.mediumChanges());
        formatMessageSection(sb, "Small changes", comparison.smallChanges());

        if (comparison.largeChanges().isEmpty()
                && comparison.mediumChanges().isEmpty()
                && comparison.smallChanges().isEmpty()) {
            sb.append("\n\nNo significant changes detected.");
        }
    }

    private static void formatMessageSection(StringBuilder sb, @Nullable String title, List<JsonMessage> messages) {
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

        if (messages.isEmpty()) return;
        if (visible.size() > 20) {
            sb.append("\n\nToo many entries to display here. View the full report on radar instead.");
            return;
        }
        if (visible.isEmpty()) {
            sb.append("\n\n").append(hidden).append(" hidden");
            return;
        }

        sb.append("\n");
        for (JsonMessage message : messages) {
            sb.append("\n- ");
            formatMessage(sb, message);
        }
        if (hidden > 0) {
            sb.append("\n\nand ").append(hidden).append(" hidden");
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

    private static void formatMessage(StringBuilder sb, JsonMessage message) {
        formatGoodness(sb, message.goodness(), true);
        for (JsonMessageSegment segment : message.segments()) {
            formatMessageSegment(sb, segment);
        }
    }

    private static void formatMessageSegment(StringBuilder sb, JsonMessageSegment segment) {
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
