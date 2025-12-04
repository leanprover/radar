package org.leanlang.radar.server.busser;

import java.util.List;
import java.util.Optional;

public sealed interface GithubBotCommand {
    record Bench() implements GithubBotCommand {}

    record BenchMathlib() implements GithubBotCommand {}

    record TooManyCommands() implements GithubBotCommand {}

    static boolean isCommand(String body) {
        return parse(body).isPresent();
    }

    static Optional<GithubBotCommand> parse(String body) {
        List<GithubBotCommand> commands =
                body.lines().flatMap(it -> parseLine(it).stream()).toList();

        if (commands.size() > 1) return Optional.of(new TooManyCommands());
        if (commands.isEmpty()) return Optional.empty();
        return Optional.of(commands.getFirst());
    }

    private static Optional<GithubBotCommand> parseLine(String line) {
        line = line.strip();
        if (line.matches("!(bench|radar)")) return Optional.of(new Bench());
        if (line.matches("!(bench|radar)\\s+mathlib4?")) return Optional.of(new BenchMathlib());
        return Optional.empty();
    }
}
