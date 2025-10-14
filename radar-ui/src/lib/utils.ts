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
