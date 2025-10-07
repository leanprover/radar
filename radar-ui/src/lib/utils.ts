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

export type QueryParamValue = LocationQueryValue | LocationQueryValue[];

export function queryParamAsString(value?: QueryParamValue): string | undefined {
  if (typeof value === "object" && value !== null) value = value[0];
  return value ?? undefined;
}

export function queryParamAsNonemptyString(value?: QueryParamValue): string | undefined {
  const s = queryParamAsString(value);
  if (s === "") return undefined;
  return s;
}

export function queryParamAsStringArray(value?: QueryParamValue): string[] {
  if (value === undefined || value === null) return [];
  if (typeof value !== "object") value = [value];
  return value.filter((it) => it !== null);
}

export function queryParamAsNonemptyStringArray(value?: QueryParamValue): string[] {
  return queryParamAsStringArray(value).filter((it) => it !== "");
}
