package org.leanlang.radar.server.busser;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public sealed interface GithubBotCommand {
    record Bench() implements GithubBotCommand {}

    record BenchMathlib() implements GithubBotCommand {}

    record TooManyCommands() implements GithubBotCommand {}

    static boolean isCommand(String body, @Nullable Pattern aliasRegex) {
        return parse(body, aliasRegex).isPresent();
    }

    static Optional<GithubBotCommand> parse(String body, @Nullable Pattern aliasRegex) {
        List<GithubBotCommand> commands =
                body.lines().flatMap(it -> parseLine(it, aliasRegex).stream()).toList();

        if (commands.size() > 1) return Optional.of(new TooManyCommands());
        if (commands.isEmpty()) return Optional.empty();
        return Optional.of(commands.getFirst());
    }

    private static Optional<GithubBotCommand> parseLine(String line, @Nullable Pattern aliasRegex) {
        line = line.strip();

        String benchPattern =
                aliasRegex == null ? "!(bench|radar)" : "!(bench|radar)\\s+(?:" + aliasRegex.pattern() + ")";
        if (line.matches(benchPattern)) return Optional.of(new Bench());

        if (line.matches("!(bench|radar)\\s+mathlib4?")) return Optional.of(new BenchMathlib());

        return Optional.empty();
    }
}
