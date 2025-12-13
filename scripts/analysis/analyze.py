import argparse
import json
from pathlib import Path

import matplotlib.pyplot as plt


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

        deltas = differentiate(values)
        abs_deltas = list(abs(d) for d in deltas)
        abs_q = compute_quantile(sorted(abs_deltas), 0.95)
        abs_limit_minor = abs_q * 3
        abs_limit_major = abs_q * 6

        w, h = 2, 1
        plt.figure(figsize=(w * 8, h * 8))
        plt.suptitle(metric)

        plt.subplot(h, w, 1)
        plt.title("values")
        plt.plot(range(len(values)), values, linestyle="-")
        plt.grid(True)
        plt.ylim(0, None)
        for di, d in enumerate(deltas):
            if abs(d) > abs_limit_major:
                plt.axvline(di + 1, color="red", linestyle="--", alpha=0.5)
            elif abs(d) > abs_limit_minor:
                plt.axvline(di + 1, color="orange", linestyle="--", alpha=0.5)

        plt.subplot(h, w, 2)
        plt.title("deltas")
        plt.scatter(
            range(len(deltas)),
            deltas,
            c=[
                "red"
                if abs(d) > abs_limit_major
                else "orange"
                if abs(d) > abs_limit_minor
                else "blue"
                for d in deltas
            ],
        )
        plt.axhline(abs_q, color="green", linestyle="--")
        plt.axhline(-abs_q, color="green", linestyle="--")
        plt.axhline(abs_limit_minor, color="orange", linestyle="--")
        plt.axhline(-abs_limit_minor, color="orange", linestyle="--")
        plt.axhline(abs_limit_major, color="red", linestyle="--")
        plt.axhline(-abs_limit_major, color="red", linestyle="--")
        plt.grid(True)

        p.parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(p)
        plt.close()
        width = len(str(len(metrics)))
        percent = 100 * i / len(metrics)
        print(f"[{i:{width}}/{len(metrics)},{percent:5.1f}%] {metric} to {p}")


if __name__ == "__main__":
    main()
