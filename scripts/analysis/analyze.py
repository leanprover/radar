import argparse
import json
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import ruptures as rpt


def compute_mean(values: list[float]) -> float:
    assert values, "need at least one value for mean"
    return sum(values) / len(values)


# Expects sorted values
def compute_variance(values: list[float]) -> float:
    assert len(values) >= 2, "need at least two values for variance"
    n = len(values)
    mu = compute_mean(values)
    return sum((x - mu) ** 2 for x in values) / (n - 1)


# Expects sorted values
def compute_stddev(values: list[float]) -> float:
    # Not completely unbiased, but good enough
    assert len(values) >= 2, "need at least two values for stddev"
    return compute_variance(values) ** 0.5


# Expects sorted values
def compute_quantile(values: list[float], p: float) -> float:
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


def smoothen(values: list[float], window: int = 3) -> list[float]:
    result = []
    for i in range(len(values) - window + 1):
        window_values = values[i : i + window]
        window_mean = compute_mean(window_values)
        result.append(window_mean)
    return result


def differentiate(values: list[float]) -> list[float]:
    return [b - a for a, b in zip(values, values[1:])]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("data", type=Path)
    args = parser.parse_args()

    datafile: Path = args.data
    outdir: Path = datafile.with_suffix(".out")

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

    print("Preparing outdir...", end="", flush=True)
    outdir.mkdir(parents=True, exist_ok=True)
    for child in outdir.iterdir():
        child.unlink()
    print(" Done.")

    print("Analyzing data...")

    by_metric = {
        metric: [by_sha[sha][metric] for sha in shas if metric in by_sha[sha]]
        for metric in metrics
    }

    w = len(str(len(metrics)))
    for i, metric in enumerate(sorted(metrics), start=1):
        topic, category = metric.split("//")
        name = f"{category.replace('/', '|')}||{topic.replace('/', '|')}"
        p = outdir / f"{name}.png"

        values = by_metric[metric]
        values.reverse()  # Old to new
        if len(values) < 10:
            continue

        algo = rpt.Pelt(model="rbf").fit(np.array(values))
        result = algo.predict(pen=10)

        plt.figure(figsize=(16, 8))
        plt.title("values")
        plt.grid(True)
        plt.plot(range(len(values)), values, linestyle="-")
        for cp in result:
            plt.axvline(cp, color="red", linestyle="--")

        p.parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(p)
        plt.close()
        print(f"[{i:{w}}/{len(metrics)}] {metric} to {p}")


if __name__ == "__main__":
    main()
