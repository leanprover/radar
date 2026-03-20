import argparse
import math
import re
from dataclasses import dataclass
from pathlib import Path

import yaml
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


def compute_deltas(values: list[float | None]) -> list[float]:
    return [
        b - a for a, b in zip(values, values[1:]) if a is not None and b is not None
    ]


def compute_delta_quantile(values: list[float], q: float) -> float:
    assert values
    assert 0 <= q <= 1

    # q=0 corresponds exactly to values[0]
    # q=1 corresponds exactly to values[-1]
    # Linearly interpolate between the two closest values

    asc = sorted(abs(v) for v in values)
    top = len(values) - 1

    x = q * top
    index = int(x)
    if index >= top:
        return asc[-1]

    weight = x - index
    return asc[index] * (1 - weight) + asc[index + 1] * weight


@dataclass
class MetricFilterConfig:
    match: re.Pattern
    check_delta_percent_small: float | None = None
    check_delta_percent_medium: float | None = None
    check_delta_percent_large: float | None = None
    check_quantile_factor_small: float | None = None
    check_quantile_factor_medium: float | None = None
    check_quantile_factor_large: float | None = None
    reduce_expected_direction_reference_category: str | None = None
    reduce_expected_direction_factor: float | None = None
    reduce_absolute_limits_small: float | None = None
    reduce_absolute_limits_medium: float | None = None


@dataclass
class RepoConfig:
    filters: list[MetricFilterConfig]
    significant_large_changes: int = 1
    significant_medium_changes: int = 5
    significant_small_changes: int = 20


def load_repo_config(config_path: Path, repo_name: str) -> RepoConfig:
    with open(config_path) as f:
        config = yaml.safe_load(f)

    for repo in config.get("repos", []):
        if repo["name"] != repo_name:
            continue

        filters = []
        for m in repo.get("significantMetrics", []):
            f = MetricFilterConfig(
                match=re.compile(m.get("match", "")),
                check_delta_percent_small=m.get("checkDeltaPercentSmall"),
                check_delta_percent_medium=m.get("checkDeltaPercentMedium"),
                check_delta_percent_large=m.get("checkDeltaPercentLarge"),
                check_quantile_factor_small=m.get("checkQuantileFactorSmall"),
                check_quantile_factor_medium=m.get("checkQuantileFactorMedium"),
                check_quantile_factor_large=m.get("checkQuantileFactorLarge"),
                reduce_expected_direction_reference_category=m.get(
                    "reduceExpectedDirectionReferenceCategory"
                ),
                reduce_expected_direction_factor=m.get("reduceExpectedDirectionFactor"),
                reduce_absolute_limits_small=m.get("reduceAbsoluteLimitsSmall"),
                reduce_absolute_limits_medium=m.get("reduceAbsoluteLimitsMedium"),
            )
            filters.append(f)

        return RepoConfig(
            filters=filters,
            significant_large_changes=repo.get("significantLargeChanges", 1),
            significant_medium_changes=repo.get("significantMediumChanges", 5),
            significant_small_changes=repo.get("significantSmallChanges", 20),
        )

    raise ValueError(f"Repo {repo_name!r} not found in config")


@dataclass
class Context:
    first: Commit
    second: Commit
    metrics: set[str]
    quantiles: dict[str, float]

    def values(self, metric: str) -> tuple[float | None, float | None]:
        first = self.first.metrics.get(metric)
        second = self.second.metrics.get(metric)
        return first, second

    def quantile(self, metric: str) -> float | None:
        return self.quantiles.get(metric)


@dataclass
class MetricSignificance:
    metric: str
    importance: int  # 0 = small, 1 = medium, 2 = large
    notes: list[str]


def importance_from_limits(
    value: float,
    small: float | None,
    medium: float | None,
    large: float | None,
) -> int | None:
    if large is not None and value >= large:
        return 2
    if medium is not None and value >= medium:
        return 1
    if small is not None and value >= small:
        return 0
    return None


def importance_max_nullable(a: int | None, b: int | None) -> int | None:
    if a is None:
        return b
    if b is None:
        return a
    return max(a, b)


class MetricComparer:
    def __init__(self, f: MetricFilterConfig, ctx: Context, metric: str) -> None:
        self.f = f
        self.ctx = ctx
        self.metric = metric

    def check_delta_percent(
        self,
        v_first: float,
        v_second: float,
    ) -> tuple[int, list[str]] | None:
        if v_first == 0:
            return None

        delta_percent = (v_second - v_first) / v_first * 100
        importance = importance_from_limits(
            abs(delta_percent),
            self.f.check_delta_percent_small,
            self.f.check_delta_percent_medium,
            self.f.check_delta_percent_large,
        )
        if importance is None:
            return None

        return importance, [f"{delta_percent:+.2f}%"]

    def check_quantile_factor(
        self,
        v_first: float,
        v_second: float,
    ) -> tuple[int, list[str]] | None:
        if v_first == v_second:
            return None  # Prevent 0.0/0.0

        qf = self.ctx.quantile(self.metric)
        if qf is None:
            return None

        factor = abs(v_second - v_first) / qf if qf != 0 else math.inf
        importance = importance_from_limits(
            factor,
            self.f.check_quantile_factor_small,
            self.f.check_quantile_factor_medium,
            self.f.check_quantile_factor_large,
        )
        if importance is None:
            return None

        notes = []
        if v_first != 0:
            delta_percent = (v_second - v_first) / v_first * 100
            notes.append(f"{delta_percent:+.2f}%")
        notes.append(f"{factor:.2f} * quantile")
        return importance, notes

    def reduce_expected_direction(
        self,
        v_first: float,
        v_second: float,
        importance: int,
        notes: list[str],
    ) -> int:
        if self.f.reduce_expected_direction_reference_category is None:
            return importance

        metric_topic, _ = self.metric.split("//")
        reference_metric = (
            f"{metric_topic}//{self.f.reduce_expected_direction_reference_category}"
        )

        rv_first, rv_second = self.ctx.values(reference_metric)
        if rv_first is None or rv_second is None:
            return importance

        m_delta = v_second - v_first
        r_delta = rv_second - rv_first
        same_dir = (r_delta > 0 and m_delta > 0) or (r_delta < 0 and m_delta < 0)
        if not same_dir:
            return importance

        if self.f.reduce_expected_direction_factor is not None:
            f_m = abs(m_delta) / v_first if v_first != 0 else math.inf
            f_r = abs(r_delta) / rv_first if rv_first != 0 else math.inf
            if f_m >= self.f.reduce_expected_direction_factor * f_r:
                return importance

        if importance > 0:
            notes.append(f"{importance}→0, expected")
            importance = 0
        return importance

    def reduce_absolute_limits(
        self,
        v_first: float,
        v_second: float,
        importance: int,
        notes: list[str],
    ) -> int:
        delta = abs(v_second - v_first)

        if (
            self.f.reduce_absolute_limits_small is not None
            and delta < self.f.reduce_absolute_limits_small
            and importance > 0
        ):
            notes.append(f"{importance}→0, small change")
            return 0
        if (
            self.f.reduce_absolute_limits_medium is not None
            and delta < self.f.reduce_absolute_limits_medium
            and importance > 1
        ):
            notes.append(f"{importance}→1, medium change")
            return 1
        return importance

    def compare(self) -> MetricSignificance | None:
        v_first, v_second = self.ctx.values(self.metric)
        if v_first is None or v_second is None:
            return None

        sig_delta = self.check_delta_percent(v_first, v_second)
        sig_quantile = self.check_quantile_factor(v_first, v_second)

        # Take the higher of the two; prefer delta on a tie (mirrors Java's maxNullable)
        if sig_delta is not None and (
            sig_quantile is None or sig_delta[0] >= sig_quantile[0]
        ):
            importance, notes = sig_delta[0], list(sig_delta[1])
        elif sig_quantile is not None:
            importance, notes = sig_quantile[0], list(sig_quantile[1])
        else:
            return None

        importance = self.reduce_expected_direction(
            v_first, v_second, importance, notes
        )
        importance = self.reduce_absolute_limits(v_first, v_second, importance, notes)

        return MetricSignificance(self.metric, importance, notes)

    @staticmethod
    def compare_metric(
        f: MetricFilterConfig,
        ctx: Context,
        metric: str,
    ) -> MetricSignificance | None:
        return MetricComparer(f, ctx, metric).compare()


def compare_commits(
    filters: list[MetricFilterConfig],
    first: Commit,
    second: Commit,
    metrics: set[str],
    quantiles: dict[str, float],
) -> list[MetricSignificance]:
    ctx = Context(first, second, metrics, quantiles)

    result = []
    for metric in metrics:
        for f in filters:
            if not f.match.search(metric):
                continue
            sig = MetricComparer.compare_metric(f, ctx, metric)
            if sig is not None:
                result.append(sig)
            break

    return result


def print_section(repo: str, name: str, comps: list[MetricSignificance]) -> None:
    if not comps:
        return
    print(f"{ANSI_BOLD}{name} ({len(comps)}):{ANSI_RESET}")
    for comp in comps:
        print(
            "  "
            + ansi_hyperlink(f"{comp.metric:100}", link_radar_graph(repo, comp.metric))
            + "".join(f" ({note})" for note in comp.notes)
        )


class Args(argparse.Namespace):
    repo: str
    config: Path
    amount: int
    skip: int


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("repo")
    parser.add_argument("--config", "-c", type=Path, required=True)
    parser.add_argument("--amount", "-n", type=int, default=30)
    parser.add_argument("--skip", "-s", type=int, default=0)
    args = parser.parse_args(namespace=Args)

    datafile: Path = Path(f"{args.repo}.jsonl")

    print(f"Loading config from {args.config}... ", end="", flush=True)
    repo_config = load_repo_config(args.config, args.repo)
    print(f"Loaded {len(repo_config.filters)} metric filters.")

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
        deltas = compute_deltas(v)
        if len(deltas) < 10:
            print(f"  Skipping metric {m} (not enough data)")
            continue
        quantiles[m] = compute_delta_quantile(deltas, 0.9)
    print(f"Got {len(quantiles)} quantiles.")

    # Select commits to analyze
    start = len(commits) - args.skip - args.amount - 1
    end = len(commits) - args.skip
    assert 0 <= start < end <= len(commits)
    commits = commits[start:end]

    # Analyze commits
    significant = 0
    for c1, c2 in zip(commits, commits[1:]):
        print()
        print(
            ANSI_BOLD
            + ANSI_YELLOW
            + ansi_hyperlink(c2.sha, link_radar_comparison(args.repo, c1.sha, c2.sha))
            + ANSI_RESET
            + " "
            + c2.title
        )

        sigs = compare_commits(repo_config.filters, c1, c2, metrics, quantiles)
        sigs.sort(key=lambda c: c.metric)

        small = [c for c in sigs if c.importance <= 0]
        medium = [c for c in sigs if c.importance == 1]
        large = [c for c in sigs if c.importance >= 2]

        n_large = len(large)
        n_medium = n_large + len(medium)
        n_small = n_medium + len(small)

        if (
            n_large >= repo_config.significant_large_changes
            or n_medium >= repo_config.significant_medium_changes
            or n_small >= repo_config.significant_small_changes
        ):
            significant += 1
            print(
                f"{ANSI_BOLD + ANSI_RED_BRIGHT}Significant changes detected.{ANSI_RESET}"
            )
        else:
            print(f"{ANSI_BOLD + ANSI_GREEN}No significant changes.{ANSI_RESET}")

        print_section(args.repo, "Large", large)
        print_section(args.repo, "Medium", medium)
        print_section(args.repo, "Small", small)

    print()
    print(f"Significant: {significant}/{len(commits) - 1}")


if __name__ == "__main__":
    main()
