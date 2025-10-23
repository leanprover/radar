import type { ClassValue } from "clsx";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// TODO Replace use sites with symmetricDifference once ES2025 or ES2026 or something is in TS
export function setsEqual<T>(a: Set<T>, b: Set<T>): boolean {
  if (a.size !== b.size) return false;
  for (const value of a.values()) if (!b.has(value)) return false;
  return true;
}

// Split a metric `<topic>//<category>` into its topic and category.
// If no `//` exists, the category is undefined.
export function parseMetric(metric: string): [string] | [string, string] {
  const i = metric.indexOf("//");
  if (i < 0) return [metric];
  return [metric.slice(0, i), metric.slice(i + 2)];
}

export function metricFilterMatches(filter: string, metric: string): boolean {
  return new RegExp(filter, "i").test(metric);
}
