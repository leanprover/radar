import type { ClassValue } from "clsx";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import type { LocationQueryValue } from "vue-router";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// TODO Replace use sites with symmetricDifference once ES2025 or ES2026 or something is in TS
export function setsEqual<T>(a: Set<T>, b: Set<T>): boolean {
  if (a.size !== b.size) return false;
  for (const value of a.values()) if (!b.has(value)) return false;
  return true;
}

export function queryParamAsString(value: LocationQueryValue | LocationQueryValue[] | undefined): string | undefined {
  if (typeof value === "object" && value !== null) value = value[0];
  return value ?? undefined;
}

export function queryParamAsNonemptyString(
  value: LocationQueryValue | LocationQueryValue[] | undefined,
): string | undefined {
  const s = queryParamAsString(value);
  if (s === "") return undefined;
  return s;
}
