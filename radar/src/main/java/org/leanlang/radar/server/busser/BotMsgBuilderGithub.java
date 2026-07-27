package org.leanlang.radar.server.busser;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.leanlang.radar.server.compare.JsonCommitComparison;
import org.leanlang.radar.server.repos.Repo;
import org.leanlang.radar.server.repos.source.RepoSourceGithub;
import org.leanlang.radar.util.GithubLinker;
import org.leanlang.radar.util.RadarLinker;

public record BotMsgBuilderGithub(RadarLinker radarLinker, GithubLinker githubLinker) {
    private static final String EDIT_POSSIBLE =
            "\n\n<sub>You can edit the original message until the command succeeds.</sub>";

    public static final String WARNINGS_EXPLANATION =
            "These warnings may indicate that the benchmark results are not directly comparable,"
                    + " for example due to changes in the runner configuration or hardware.";

    public String msgNotInPr() {
        return "This command can only be used in pull requests.";
    }

    public String msgNotPermitted() {
        return "You are not permitted to use this command.";
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

    public String msgRepoIsNotMathlib(String repo) {
        return "This command can only be used in the " + repo + " repository." + EDIT_POSSIBLE;
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
            boolean isRepeat,
            JsonCommitComparison comparison) {

        StringBuilder sb = new StringBuilder();

        sb.append("[Benchmark results](")
                .append(radarLinker.comparison(repo.name(), chashFirst, chashSecond))
                .append(") for ")
                .append(linkToChash(repoForeign ? repo : null, chashSecond))
                .append(" against ")
                .append(linkToChash(repoForeign ? repo : null, chashFirst))
                .append(" are in.");

        if (isRepeat) sb.append(" (These commits have already been benchmarked in a previous command.)");

        if (comparison.significant()) sb.append(" There are significant results.");
        else sb.append(" No significant results found.");

        Stream.concat(Stream.of(userLogin), usersThatReactedWithEye.stream()).collect(Collectors.toSet()).stream()
                .sorted()
                .forEach(it -> sb.append(" @").append(it));

        formatWarnings(sb, comparison);
        new BotMsgBuilder(radarLinker, repo.name(), chashSecond).formatBody(sb, comparison);

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
}
