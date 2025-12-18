package org.leanlang.radar.server.busser;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.compare.JsonMessageSegment;
import org.leanlang.radar.server.compare.JsonSignificance;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.source.RepoSourceGithub;
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

    public String labelMismatch(List<String> superfluousLabels, List<String> missingLabels) {
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

    public String failedToFindMergeBase() {
        return "Failed to find a commit to compare against." + EDIT_POSSIBLE;
    }

    public String linkToChash(@Nullable Repo repo, String chash) {
        if (repo == null) return chash;
        if (repo.source() instanceof RepoSourceGithub(String ghOwner, String ghRepo))
            return new GithubLinker(ghOwner, ghRepo).commit(chash).toString();
        return chash;
    }

    public String inProgress(Repo repo, boolean repoForeign, String chashFirst, String chashSecond) {
        return "Benchmarking "
                + linkToChash(repoForeign ? repo : null, chashSecond)
                + " ([status](" + radarLinker.commit(repo.name(), chashSecond) + "))"
                + " against "
                + linkToChash(repoForeign ? repo : null, chashFirst)
                + " ([status](" + radarLinker.commit(repo.name(), chashFirst) + "))"
                + ".\n\n"
                + "<sub>React with :eyes: to be notified when the results are in."
                + " The command author is always notified.</sub>";
    }

    public String finished(
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

        List<JsonSignificance> significantRuns = getSignificantRuns(comparison);
        List<JsonSignificance> significantLargeMetrics =
                getSignificantMetrics(comparison, JsonSignificance.IMPORTANCE_LARGE);
        List<JsonSignificance> significantMediumMetrics =
                getSignificantMetrics(comparison, JsonSignificance.IMPORTANCE_MEDIUM);
        List<JsonSignificance> significantSmallMetrics =
                getSignificantMetrics(comparison, JsonSignificance.IMPORTANCE_SMALL);

        formatSignificanceSection(sb, "Runs", significantRuns);
        formatSignificanceSection(sb, "Large changes", significantLargeMetrics);
        formatSignificanceSection(sb, "Medium changes", significantMediumMetrics);
        formatSignificanceSection(sb, "Small changes", significantSmallMetrics);

        if (significantRuns.isEmpty()
                && significantLargeMetrics.isEmpty()
                && significantMediumMetrics.isEmpty()
                && significantSmallMetrics.isEmpty()) {
            sb.append("\n\nNo significant changes detected.");
        }

        return sb.toString();
    }

    private void formatSignificanceSection(StringBuilder sb, String name, List<JsonSignificance> significances) {
        if (significances.isEmpty()) return;

        if (significances.size() > 10) sb.append("\n<details>\n");
        else sb.append("\n<details open>\n");

        sb.append("<summary>")
                .append(name)
                .append(" (")
                .append(significances.size())
                .append(")")
                .append("</summary>\n");

        // If there's no empty line between the <summary> and the list, GitHub won't render it correctly.
        sb.append("\n");

        for (JsonSignificance significance : significances) {
            sb.append("- ");
            formatMessage(sb, significance);
            sb.append("\n");
        }

        sb.append("</details>");
    }

    public static List<JsonSignificance> getSignificantRuns(JsonCommitComparison comparison) {
        return comparison.runSignificances().toList();
    }

    public static List<JsonSignificance> getSignificantMetrics(JsonCommitComparison comparison, int importance) {
        return comparison
                .metricSignificances()
                .filter(it -> it.importance() == importance)
                .toList();
    }

    public static void formatMessage(StringBuilder sb, JsonSignificance message) {
        formatMessageGoodness(sb, message.goodness(), true);
        for (JsonMessageSegment segment : message.segments()) {
            formatMessageSegment(sb, segment);
        }
    }

    private static void formatMessageGoodness(StringBuilder sb, int goodness, boolean trailingSpace) {
        if (goodness < 0) {
            sb.append("\uD83D\uDFE5");
            if (trailingSpace) sb.append(" ");
        } else if (goodness > 0) {
            sb.append("✅");
            if (trailingSpace) sb.append(" ");
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
