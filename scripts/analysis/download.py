import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import requests


@dataclass
class Commit:
    sha: str
    title: str
    metrics: dict[str, float]

    @classmethod
    def from_soup(cls, data: Any) -> "Commit":
        return cls(
            sha=data["sha"],
            title=data["title"],
            metrics=data["metrics"],
        )

    def to_soup(self) -> Any:
        return {
            "sha": self.sha,
            "title": self.title,
            "metrics": self.metrics,
        }

    @classmethod
    def load_all(cls, path: Path) -> list["Commit"]:
        data = json.loads(path.read_text())
        return [cls.from_soup(item) for item in data]

    @staticmethod
    def save_all(commits: list["Commit"], path: Path) -> None:
        data = [commit.to_soup() for commit in commits]
        path.write_text(json.dumps(data))


def get_recent_commits(instance: str, repo: str, n: int) -> list[Commit]:
    r = requests.get(f"{instance}/repos/{repo}/history", params={"n": n})
    r.raise_for_status()
    result = []
    for entry in r.json()["entries"]:  # Recent to old
        result.append(
            Commit(
                sha=entry["commit"]["chash"],
                title=entry["commit"]["title"],
                metrics={},
            )
        )
    return result


def get_metrics_for_commit(instance: str, repo: str, commit: Commit) -> None:
    r = requests.get(f"{instance}/compare/{repo}/parent/{commit.sha}")
    r.raise_for_status()
    for m in r.json()["comparison"]["metrics"]:
        if m.get("second") is not None:
            commit.metrics[m["metric"]] = m["second"]


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
    commits = get_recent_commits(instance, repo, amount)

    print("Downloading data")
    w = len(str(len(commits)))
    for i, commit in enumerate(commits, 1):
        print(f"[{i:{w}}/{len(commits)}] {commit.sha} {commit.title}")
        get_metrics_for_commit(instance, repo, commit)

    print(f"Saving data to {out}")
    Commit.save_all(commits, out)


if __name__ == "__main__":
    main()
