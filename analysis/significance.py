import argparse
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Self

from download import Commit

ANSI_BOLD: str = "\033[1m"
ANSI_GREEN: str = "\033[1;32m"
ANSI_YELLOW: str = "\033[1;33m"
ANSI_RED: str = "\033[1;31m"
ANSI_RED_BRIGHT: str = "\033[1;91m"
ANSI_RESET: str = "\033[0m"


def ansi_hyperlink(text: str, url: str) -> str:
    return f"\033]8;;{url}\033\\{text}\033]8;;\033\\"


RADAR_URL = "https://radar.lean-lang.org"


def link_radar_comparison(repo: str, first: str, second: str) -> str:
    return f"{RADAR_URL}/repos/{repo}/commits/{second}?reference={first}"


def link_radar_graph(repo: str, metric: str) -> str:
    return f"{RADAR_URL}/repos/{repo}/graph?m={metric}"


##########
## Misc ##
##########

IMPORTANCE_SMALL = 0
IMPORTANCE_MEDIUM = 1
IMPORTANCE_LARGE = 2


@dataclass
class Context:
    repo: str
    first: Commit
    second: Commit
    metrics: set[str]
    quantiles: dict[str, float]

    def values(self, metric: str) -> tuple[float, float]:
        if (first := self.first.metrics.get(metric)) is None:
            raise ValueError(f"Metric {metric} missing in first commit")
        if (second := self.second.metrics.get(metric)) is None:
            raise ValueError(f"Metric {metric} missing in second commit")
        return first, second

    def quantile(self, metric: str) -> float:
        if (quantile := self.quantiles.get(metric)) is None:
            raise ValueError(f"Quantile for metric {metric} missing")
        return quantile


@dataclass
class MetricSignificance:
    metric: str
    importance: int  # 0 = small, 1 = medium, 2 = large
    notes: list[str]

    @classmethod
    def with_limits(
        cls,
        metric: str,
        value: float,
        small: float,
        medium: float,
        large: float,
    ) -> Self | None:
        if value >= large:
            return cls(metric, 2, [])
        elif value >= medium:
            return cls(metric, 1, [])
        elif value >= small:
            return cls(metric, 0, [])

    def add_note(self, note: str) -> None:
        self.notes.append(note)


@dataclass
class MetricComparison:
    metric: str
    result: MetricSignificance | None


#######################
## MetricFilter.java ##
#######################


@dataclass
class MetricFilter:
    match: str = ""
    direction: int = 0

    check_delta_percent_small: float | None = None
    check_delta_percent_medium: float | None = None
    check_delta_percent_large: float | None = None

    check_quantile_factor_small: float | None = None
    check_quantile_factor_medium: float | None = None
    check_quantile_factor_large: float | None = None

    reduce_expected_direction_reference_category: str | None = None

    reduce_absolute_limits_small: float | None = None
    reduce_absolute_limits_medium: float | None = None


##########################
## QuantileUpdater.java ##
##########################


def compute_deltas(values: list[float | None]) -> list[float]:
    deltas = []
    for i in range(len(values) - 1):
        first = values[i]
        second = values[i + 1]
        if first is None or second is None:
            continue
        deltas.append(second - first)
    return deltas


def compute_abs_quantile(deltas: list[float], quantile: float) -> float | None:
    if len(deltas) < 10:
        return None
    quantile = max(0.0, min(1.0, quantile))

    values: list[float] = list(sorted(abs(d) for d in deltas))
    top: int = len(values) - 1

    x: float = quantile * top
    index: int = round(x)
    if index >= top:
        return values[top]

    weight: float = x - index
    return values[index] * (1 - weight) + values[index + 1] * weight


def compute_quantile_for_metric(values: list[float | None]) -> float | None:
    deltas = compute_deltas(values)
    return compute_abs_quantile(deltas, 0.9)


#########################
## MetricComparer.java ##
#########################


class MetricComparer:
    def __init__(self, metric_filter: MetricFilter) -> None:
        self.metric_filter = metric_filter

        self.significance: MetricSignificance | None = None

    def check_delta_percent(self, v_first: float, v_second: float) -> None:
        large_delta = self.metric_filter.check_delta_percent_large
        medium_delta = self.metric_filter.check_delta_percent_medium
        small_delta = self.metric_filter.check_delta_percent_small
        if large_delta is None and medium_delta is None and small_delta is None:
            return

        if v_first == 0:
            return
        delta_percent = (v_second - v_first) / v_first * 100
        delta_percent_abs = abs(delta_percent)

        is_large = large_delta is not None and delta_percent_abs
        is_medium = medium_delta is not None and delta_percent_abs
        is_small = small_delta is not None and delta_percent_abs

        importance: int
        if is_large:
            importance = IMPORTANCE_LARGE
        elif is_medium:
            importance = IMPORTANCE_MEDIUM
        elif is_small:
            importance = IMPORTANCE_SMALL
        else:
            return


#####################
## Remaining logic ##
#####################


type Step = Callable[[Context, MetricComparison], None]


def check_delta_percent(small: float, medium: float, large: float) -> Step:
    def step(ctx: Context, comp: MetricComparison) -> None:
        v1, v2 = ctx.values(comp.metric)
        if v1 == 0:
            return

        delta_percent = (v2 - v1) / v1 * 100

        sig = MetricSignificance.with_limits(
            comp.metric,
            abs(delta_percent),
            small,
            medium,
            large,
        )

        if sig is not None:
            comp.result = sig
            comp.result.add_note(f"{delta_percent:+.2f}%")

    return step


def check_quantile_factor(small: float, medium: float, large: float) -> Step:
    def step(ctx: Context, comp: MetricComparison) -> None:
        v1, v2 = ctx.values(comp.metric)
        qf = ctx.quantile(comp.metric)
        if qf == 0:
            return

        f = abs(v2 - v1) / qf
        sig = MetricSignificance.with_limits(comp.metric, f, small, medium, large)

        if sig is not None:
            comp.result = sig
            if v1 != 0:
                delta_percent = (v2 - v1) / v1 * 100
                comp.result.add_note(f"{delta_percent:+.2f}%")
            comp.result.add_note(f"{f:.2f} * quantile")

    return step


def reduce_expected_direction(reference_category: str) -> Step:
    def step(ctx: Context, comp: MetricComparison) -> None:
        if comp.result is None:
            return

        metric_topic, _ = comp.metric.split("//")
        reference_metric = f"{metric_topic}//{reference_category}"

        mv1, mv2 = ctx.values(comp.metric)
        rv1, rv2 = ctx.values(reference_metric)

        m_delta = mv2 - mv1
        r_delta = rv2 - rv1
        expected = (r_delta > 0 and m_delta > 0) or (r_delta < 0 and m_delta < 0)
        if expected and comp.result.importance > 0:
            comp.result.add_note(f"{comp.result.importance}→0, expected")
            comp.result.importance = 0

    return step


def reduce_absolute_limits(
    self,
    small: float | None = None,
    medium: float | None = None,
) -> Step:
    def step(ctx: Context, comp: MetricComparison) -> None:
        if comp.result is None:
            return

        v1, v2 = ctx.values(comp.metric)
        delta = abs(v2 - v1)

        if small is not None and delta < small and comp.result.importance > 0:
            comp.result.add_note(f"{comp.result.importance}→0, small change")
            comp.result.importance = 0
        elif medium is not None and delta < medium and comp.result.importance > 1:
            comp.result.add_note(f"{comp.result.importance}→1, medium change")
            comp.result.importance = 1

    return step


def steps_for_metric(repo: str, metric: str) -> list[Step]:
    if repo == "lean4":
        if re.search(r"^build/module/.*//instructions$", metric):
            return [
                check_quantile_factor(5, 5 * 3, 5 * 3 * 3),
                reduce_expected_direction("lines"),
                reduce_absolute_limits(1000 * 1000 * 1000, 5 * 1000 * 1000 * 1000),
            ]
        if re.search(r"//instructions$", metric):
            return [
                check_quantile_factor(5, 5 * 3, 5 * 3 * 3),
                reduce_expected_direction("lines"),
            ]
        if re.search(r"//lines$|^build/stat/", metric):
            return []
        if re.search(r"//bytes \.(ilean|olean|olean\.server|olean\.private)$", metric):
            return [
                check_delta_percent(10, 20, 50),
                reduce_expected_direction("lines"),
            ]
        if re.search(r"", metric):
            return []

    elif repo == "mathlib4":
        if re.search(r"^build/module/.*//instructions$", metric):
            return [
                check_quantile_factor(10, 10 * 3, 10 * 3 * 3),
                reduce_expected_direction("lines"),
                reduce_absolute_limits(1000 * 1000 * 1000, 5 * 1000 * 1000 * 1000),
            ]
        if re.search(r"//instructions$", metric):
            return [
                check_quantile_factor(10, 10 * 3, 10 * 3 * 3),
                reduce_expected_direction("lines"),
            ]
        if re.search(r"//wall-clock$", metric):
            return [
                check_quantile_factor(3, 3 * 2, 3 * 3),
            ]

    return []


def compare_commits(
    repo: str,
    first: Commit,
    second: Commit,
    metrics: set[str],
    quantiles: dict[str, float],
) -> list[MetricSignificance]:
    ctx = Context(repo, first, second, metrics, quantiles)

    result = []
    for metric in metrics:
        comp = MetricComparison(metric, None)

        for step in steps_for_metric(repo, metric):
            try:
                step(ctx, comp)
            except Exception:
                pass

        if comp.result is not None:
            result.append(comp.result)

    return result


def print_section(repo: str, name: str, comps: list[MetricSignificance]):
    if not comps:
        return
    print(f"{ANSI_BOLD}{name} ({len(comps)}):{ANSI_RESET}")
    for comp in comps:
        print(
            "  "
            + ansi_hyperlink(f"{comp.metric:100}", link_radar_graph(repo, comp.metric))
            + "".join(f" ({note})" for note in comp.notes)
        )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("repo")
    parser.add_argument("--amount", "-n", type=int, default=30)
    parser.add_argument("--skip", "-s", type=int, default=0)
    args = parser.parse_args()

    repo: str = args.repo
    amount: int = args.amount
    skip: int = args.skip
    datafile: Path = Path(f"{repo}.jsonl")

    print(f"Loading commits from {datafile}... ", end="", flush=True)
    commits = Commit.load_all(datafile)
    print(f"Loaded {len(commits)} commits.")

    print("Transforming data... ", end="", flush=True)
    metrics = {m for c in commits for m in c.metrics.keys()}
    values = {m: [c.metrics.get(m) for c in commits] for m in metrics}
    print(f"Got values for {len(metrics)} metrics.")

    print("Computing quantiles... ", end="", flush=True)
    quantiles = {}
    for m, v in values.items():
        quantile = compute_quantile_for_metric(v)
        if quantile is not None:
            quantiles[m] = quantile
    print(f"Got {len(quantiles)} quantiles.")

    # Select commits to analyze
    start = len(commits) - skip - amount - 1
    end = len(commits) - skip
    assert 0 <= start < end <= len(commits)
    commits = commits[start:end]

    # Analyze commits
    significant = 0
    for c1, c2 in zip(commits, commits[1:]):
        print()
        print(
            ANSI_BOLD
            + ANSI_YELLOW
            + ansi_hyperlink(c2.sha, link_radar_comparison(repo, c1.sha, c2.sha))
            + ANSI_RESET
            + " "
            + c2.title
        )

        sigs = compare_commits(repo, c1, c2, metrics, quantiles)
        sigs.sort(key=lambda c: c.metric)

        small = [c for c in sigs if c.importance <= 0]
        medium = [c for c in sigs if c.importance == 1]
        large = [c for c in sigs if c.importance >= 2]

        n_large = len(large)
        n_medium = n_large + len(medium)
        n_small = n_medium + len(small)

        if n_large >= 1 or n_medium >= 5 or n_small >= 20:
            significant += 1
            print(
                f"{ANSI_BOLD + ANSI_RED_BRIGHT}Significant changes detected.{ANSI_RESET}"
            )
        else:
            print(f"{ANSI_BOLD + ANSI_GREEN}No significant changes.{ANSI_RESET}")

        print_section(repo, "Large", large)
        print_section(repo, "Medium", medium)
        print_section(repo, "Small", small)

    print()
    print(f"Significant: {significant}/{len(commits) - 1}")


if __name__ == "__main__":
    main()
