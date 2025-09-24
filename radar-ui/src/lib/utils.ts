import type { ClassValue } from "clsx";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { Temporal } from "temporal-polyfill";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDuration(duration: Temporal.Duration): string {
  let rounded = duration.round({ smallestUnit: "seconds", largestUnit: "days" });
  if (rounded.sign < 0) rounded = rounded.negated();

  const result = [];
  if (rounded.days > 0) result.push(`${rounded.days.toFixed()}d`);
  if (rounded.hours > 0) result.push(`${rounded.hours.toFixed()}h`);
  if (rounded.minutes > 0) result.push(`${rounded.minutes.toFixed()}m`);
  if (rounded.seconds > 0 || result.length === 0) result.push(`${rounded.seconds.toFixed()}s`);
  return (duration.sign < 0 ? "-" : "") + result.join(" ");
}
