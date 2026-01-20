import type { Direction } from "@/api/types.ts";
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

// TODO Replace with RegExp.escape once ES2025 or ES2026 or something is in TS
export function escapeRegex(regex: string): string {
  // https://github.com/colinhacks/zod/blob/644a08203ebb00e23484b3f9a986ae783ce26a9a/packages/zod/src/v4/core/util.ts#L470C3-L470C53
  return regex.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export function escapeMetrics(metrics: string[]): string {
  if (metrics.length === 0) return "";
  const inner = metrics.map((it) => escapeRegex(it)).join("|");
  if (metrics.length === 1) return `^${inner}$`;
  return `^(${inner})$`;
}

// Split a metric `<topic>//<category>` into its topic and category.
// If no `//` exists, the category is undefined.
export function parseMetric(metric: string): [string] | [string, string] {
  const i = metric.indexOf("//");
  if (i < 0) return [metric];
  return [metric.slice(0, i), metric.slice(i + 2)];
}

export function metricFilterMatches(filter: string, metric: string): boolean {
  try {
    return new RegExp(filter, "i").test(metric);
  } catch {
    // Invalid regex syntax
    return false;
  }
}

export type Grade = "good" | "bad" | "neutral";
export function getGrade(delta: number | [number, number], direction: Direction): Grade {
  if (typeof delta !== "number") delta = delta[1] - delta[0];
  const sign = Math.sign(delta);
  if (sign === direction) return "good";
  if (sign === -direction) return "bad";
  return "neutral";
}

export function radarTitle(...parts: string[]): string {
  parts.push("Radar");
  return parts.join(" - ");
}
