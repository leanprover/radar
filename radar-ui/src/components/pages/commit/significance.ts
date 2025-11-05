import { type JsonCommitComparison, type JsonMessageSegment } from "@/api/types.ts";

export interface ComparisonSignificance {
  major: number;
  minor: number;
  runs: JsonMessageSegment[][];
  metricsMajor: JsonMessageSegment[][];
  metricsMinor: JsonMessageSegment[][];
}

export function comparisonSignificance(comparison?: JsonCommitComparison): ComparisonSignificance {
  if (comparison === undefined) return { major: 0, minor: 0, runs: [], metricsMajor: [], metricsMinor: [] };

  const runSignificances = comparison.runs.map((it) => it.significance).filter((it) => it !== undefined);
  const metricSignificances = comparison.metrics.map((it) => it.significance).filter((it) => it !== undefined);
  const allSignificances = runSignificances.concat(metricSignificances);

  const major = allSignificances.filter((it) => it.major).length;
  const minor = allSignificances.length - major;

  const runs = comparison.runs
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .map((it) => it.message);

  const metricsMajor = comparison.metrics
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => it.major)
    .map((it) => it.message);

  const metricsMinor = comparison.metrics
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => !it.major)
    .map((it) => it.message);

  return { major, minor, runs, metricsMajor, metricsMinor };
}
