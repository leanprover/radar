import argparse
import json
from pathlib import Path

import matplotlib.pyplot as plt


def mean(values: list[float]) -> float:
    assert values, "need at least one value for mean"
    return sum(values) / len(values)


def variance(values: list[float]) -> float:
    assert len(values) >= 2, "need at least two values for variance"
    n = len(values)
    mu = mean(values)
    return sum((x - mu) ** 2 for x in values) / (n - 1)


def stddev(values: list[float]) -> float:
    # Not completely unbiased, but good enough
    assert len(values) >= 2, "need at least two values for stddev"
    return variance(values) ** 0.5


def quantile(values: list[float], p: float) -> float:
    # https://en.wikipedia.org/wiki/Percentile#Third_variant,_C_=_0
    assert values, "need at least one value for percentile"
    n = len(values)
    x = (n + 1) * p
    if x <= 1:  # C=0
        return values[0]
    if x >= n:
        return values[-1]
    lower = int(x)
    upper = lower + 1
    weight = x - lower
    return values[lower - 1] * (1 - weight) + values[upper - 1] * weight


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("data", type=Path)
    args = parser.parse_args()

    datafile: Path = args.data

    print("Loading data...", end="", flush=True)
    with datafile.open() as f:
        data = json.load(f)
        shas: list[str] = data["shas"]
        by_sha: dict[str, dict[str, float]] = data["values"]
    print(" Done.")

    print("Finding metrics...", end="", flush=True)
    # metrics = {m for c in by_sha.values() for m in c if m.endswith("//wall-clock")}
    metrics = {m for c in by_sha.values() for m in c}
    print(f" Found {len(metrics)} metrics.")

    print("Analyzing data...")

    by_metric = {
        metric: [by_sha[sha][metric] for sha in shas if metric in by_sha[sha]]
        for metric in metrics
    }

    for metric in sorted(metrics):
        topic, category = metric.split("//")
        name = f"{category.replace('/', '|')}||{topic.replace('/', '|')}"
        p = Path() / "out" / f"{name}.png"
        values = by_metric[metric]
        values.reverse()  # Old to new
        deltas = [b - a for a, b in zip(values, values[1:])]

        if len(values) < 10:
            continue

        v_mean = mean(sorted(values))
        v_stddev = stddev(sorted(values))
        v_z_scores = [(v - v_mean) / v_stddev for v in values if v_stddev != 0]

        v_q1 = quantile(sorted(values), 0.25)
        v_q3 = quantile(sorted(values), 0.75)
        v_q_lower = v_q1 - (1.5 * (v_q3 - v_q1))
        v_q_upper = v_q3 + (1.5 * (v_q3 - v_q1))

        d_mean = mean(sorted(deltas))
        d_stddev = stddev(sorted(deltas))
        # d_z_scores = [(d - d_mean) / d_stddev for d in deltas if v_stddev != 0]
        d_z_scores = [d / d_stddev for d in deltas if v_stddev != 0]

        d_q1 = quantile(sorted(deltas), 0.25)
        d_q3 = quantile(sorted(deltas), 0.75)
        d_q_lower = d_q1 - (1.5 * (d_q3 - d_q1))
        d_q_upper = d_q3 + (1.5 * (d_q3 - d_q1))

        plt.figure(figsize=(16, 16))
        plt.suptitle(metric)

        plt.subplot(2, 2, 1)
        plt.scatter(range(len(v_z_scores)), v_z_scores, alpha=0.5)
        plt.title("z-scores")
        plt.axhline(0, color="red", linestyle="--")
        plt.grid(True)
        plt.ylim(-5, 5)

        plt.subplot(2, 2, 2)
        plt.scatter(range(len(values)), values, alpha=0.5)
        plt.title("quartile-normalized")
        plt.axhline(v_q1, color="blue", linestyle="--")
        plt.axhline(v_q3, color="blue", linestyle="--")
        plt.axhline(v_q_lower, color="red", linestyle="--")
        plt.axhline(v_q_upper, color="red", linestyle="--")
        plt.grid(True)

        plt.subplot(2, 2, 3)
        plt.scatter(range(len(d_z_scores)), d_z_scores, alpha=0.5)
        plt.title("delta z-scores")
        plt.axhline(0, color="red", linestyle="--")
        plt.grid(True)
        plt.ylim(-5, 5)

        plt.subplot(2, 2, 4)
        plt.scatter(range(len(deltas)), deltas, alpha=0.5)
        plt.title("quartile-normalized deltas")
        plt.axhline(d_q1, color="blue", linestyle="--")
        plt.axhline(d_q3, color="blue", linestyle="--")
        plt.axhline(d_q_lower, color="red", linestyle="--")
        plt.axhline(d_q_upper, color="red", linestyle="--")
        plt.grid(True)

        p.parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(p)
        plt.close()
        print(f"Saved plots for {metric} to {p}")


if __name__ == "__main__":
    main()
