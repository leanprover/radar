import { type JsonCommitComparison, type JsonSignificance } from "@/api/types.ts";

export interface ComparisonSignificance {
  large: number;
  medium: number;
  small: number;
  runs: JsonSignificance[];
  metricsLarge: JsonSignificance[];
  metricsMedium: JsonSignificance[];
  metricsSmall: JsonSignificance[];
}

export function comparisonSignificance(comparison?: JsonCommitComparison): ComparisonSignificance {
  if (comparison === undefined)
    return { large: 0, medium: 0, small: 0, runs: [], metricsLarge: [], metricsMedium: [], metricsSmall: [] };

  const runSignificances = comparison.runs.map((it) => it.significance).filter((it) => it !== undefined);
  const metricSignificances = comparison.metrics.map((it) => it.significance).filter((it) => it !== undefined);
  const allSignificances = runSignificances.concat(metricSignificances);

  const large = allSignificances.filter((it) => it.importance === 2).length;
  const medium = allSignificances.filter((it) => it.importance === 1).length;
  const small = allSignificances.filter((it) => it.importance === 0).length;

  const runs = comparison.runs.map((it) => it.significance).filter((it) => it !== undefined);

  const metricsLarge = comparison.metrics
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => it.importance === 2);

  const metricsMedium = comparison.metrics
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => it.importance === 1);

  const metricsSmall = comparison.metrics
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => it.importance === 0);

  return { large, medium, small, runs, metricsLarge, metricsMedium, metricsSmall };
}
