import argparse
import json
from pathlib import Path

import requests


def get_recent_shas(instance: str, repo: str, n: int) -> list[str]:
    r = requests.get(f"{instance}/repos/{repo}/history", params={"n": n})
    r.raise_for_status()
    return [e["commit"]["chash"] for e in r.json()["entries"]]  # Recent to old


def get_metrics_for_commit(instance: str, repo: str, sha: str) -> dict[str, float]:
    r = requests.get(f"{instance}/compare/{repo}/parent/{sha}")
    r.raise_for_status()
    return {
        m["metric"]: m["second"]
        for m in r.json()["comparison"]["metrics"]
        if m.get("second") is not None
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("repo")
    parser.add_argument("--instance", default="https://radar.lean-lang.org/api/")
    parser.add_argument("--amount", "-n", type=int, default=100)
    parser.add_argument("--out", "-o", type=Path)
    args = parser.parse_args()

    repo: str = args.repo
    instance: str = args.instance.rstrip("/")
    amount: int = args.amount
    out: Path | None = args.out

    if out is None:
        out = Path(f"{repo}.json")

    print("Retrieving shas")
    shas = get_recent_shas(instance, repo, amount)

    print("Downloading data")
    w = len(str(len(shas)))
    values = {}
    for i, sha in enumerate(shas, 1):
        print(f"[{i:{w}}/{len(shas)}] {sha}")
        values[sha] = get_metrics_for_commit(instance, repo, sha)

    print(f"Saving data to {out}")
    data = {"shas": shas, "values": values}
    out.write_text(json.dumps(data))


if __name__ == "__main__":
    main()
