import type { ClassValue } from "clsx";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { Temporal } from "temporal-polyfill";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatInstant(instant: Temporal.Instant): string {
  const time = instant.toZonedDateTimeISO(Temporal.Now.timeZoneId());
  const year = time.year.toFixed().padStart(4, "0");
  const month = time.month.toFixed().padStart(2, "0");
  const day = time.day.toFixed().padStart(2, "0");
  const hour = time.hour.toFixed().padStart(2, "0");
  const minute = time.minute.toFixed().padStart(2, "0");
  const second = time.second.toFixed().padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

export function formatDuration(duration: Temporal.Duration, opts?: { sign?: boolean }): string {
  const { sign = false } = opts ?? {};
  let rounded = duration.round({ smallestUnit: "milliseconds", largestUnit: "days" });
  if (rounded.sign < 0) rounded = rounded.negated();

  const result = [];
  if (rounded.days > 0) result.push(`${rounded.days.toFixed()}d`);
  if (rounded.hours > 0) result.push(`${rounded.hours.toFixed()}h`);
  if (rounded.minutes > 0) result.push(`${rounded.minutes.toFixed()}m`);
  if (rounded.seconds > 0) result.push(`${rounded.seconds.toFixed()}s`);
  // Only show ms if no higher unit exists
  if (rounded.milliseconds > 0 && result.length === 0) result.push(`${rounded.milliseconds.toFixed()}ms`);
  if (result.length === 0) result.push("0s"); // Never show nothing
  return (duration.sign < 0 ? "-" : sign ? "+" : "") + result.join(" ");
}

export const decimalPrefixes: [string, number][] = [
  ["E", 1000 ** 6], // exa
  ["P", 1000 ** 5], // peta
  ["T", 1000 ** 4], // tera
  ["G", 1000 ** 3], // giga
  ["M", 1000 ** 2], // mega
  ["k", 1000 ** 1], // kilo
  ["", 1000 ** 0], // none
  ["m", 1000 ** -1], // milli
  ["μ", 1000 ** -2], // micro
  ["n", 1000 ** -3], // nano
  ["p", 1000 ** -4], // pico
  ["f", 1000 ** -5], // femto
];

export const binaryPrefixes: [string, number][] = [
  ["Ei", 1024 ** 6], // exbi
  ["Pi", 1024 ** 5], // pebi
  ["Ti", 1024 ** 4], // tebi
  ["Gi", 1024 ** 3], // gibi
  ["Mi", 1024 ** 2], // mebi
  ["Ki", 1024 ** 1], // kibi
];

export function formatNumber(
  number: number,
  opts?: { precision?: number; prefixes?: [string, number][]; align?: boolean; sign?: boolean },
): string {
  const { precision = 1, prefixes = decimalPrefixes, align = false, sign = false } = opts ?? {};
  const signPrefix = sign && number > 0 ? "+" : "";
  const suffixWidth = align ? Math.max(0, ...prefixes.map((it) => it[0].length)) : 0;

  const magnitude = Math.abs(number);
  for (const [suffix, value] of prefixes) {
    if (magnitude >= value) {
      return signPrefix + (number / value).toFixed(precision) + suffix.padEnd(suffixWidth);
    }
  }

  return signPrefix + number.toFixed(precision) + "".padEnd(suffixWidth);
}

export function formatBytes(bytes: number, opts?: { align?: boolean; sign?: boolean }): string {
  bytes = Math.round(bytes); // Ensure we have an integer amount of bytes
  const magnitude = Math.abs(bytes);
  const { align = false, sign = false } = opts ?? {};

  let result;
  if (magnitude > 1024 ** 6)
    result = (bytes / 1024 ** 6).toFixed() + " EiB"; // exbi
  else if (magnitude > 1024 ** 5)
    result = (bytes / 1024 ** 5).toFixed() + " PiB"; // pebi
  else if (magnitude > 1024 ** 4)
    result = (bytes / 1024 ** 4).toFixed() + " TiB"; // tebi
  else if (magnitude > 1024 ** 3)
    result = (bytes / 1024 ** 3).toFixed() + " GiB"; // gibi
  else if (magnitude > 1024 ** 2)
    result = (bytes / 1024 ** 2).toFixed() + " MiB"; // mebi
  else if (magnitude > 1024 ** 1)
    result = (bytes / 1024 ** 1).toFixed() + " KiB"; // kibi
  else result = bytes.toFixed() + (align ? "   B" : " B"); // bytes

  if (sign && bytes > 0) result = "+" + result;

  return result;
}

export function formatValue(
  value: number,
  unit?: string,
  opts?: { precision?: number; align?: boolean; sign?: boolean },
): string {
  opts = opts ?? {};
  if (unit === "s") return formatDuration(Temporal.Duration.from({ milliseconds: Math.round(value * 1000) }), opts);
  if (unit === "B") return formatBytes(value, opts);
  return formatNumber(value, opts);
}
