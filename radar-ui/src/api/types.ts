import { Temporal } from "temporal-polyfill";
import * as z from "zod";

export const Timestamp = z.number().transform((it) => Temporal.Instant.fromEpochMilliseconds(Math.round(it * 1000)));

export const JsonRun = z.object({
  name: z.string(),
  script: z.string(),
  runner: z.string(),
  active: z
    .object({ startTime: Timestamp })
    .nullish()
    .transform((it) => it ?? undefined),
  finished: z
    .object({ startTime: Timestamp, endTime: Timestamp, exitCode: z.int() })
    .nullish()
    .transform((it) => it ?? undefined),
});

export const JsonOutputLine = z
  .tuple([Timestamp, z.int(), z.string()])
  .transform(([time, source, line]) => ({ time, source, line }));

export const JsonCommitIdent = z.object({
  name: z.string(),
  email: z.string(),
  time: Timestamp,
  offset: z.int(),
});

export const JsonCommit = z.object({
  chash: z.string(),
  author: JsonCommitIdent,
  committer: JsonCommitIdent,
  title: z.string(),
  body: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
});

export type JsonMessageSegment =
  | { type: "delta"; amount: number; unit?: string }
  | { type: "deltaPercent"; factor: number }
  | { type: "metric"; metric: string }
  | { type: "text"; text: string };
export const JsonMessageSegment = z.discriminatedUnion("type", [
  z.object({
    type: z.literal("delta"),
    amount: z.number(),
    unit: z
      .string()
      .nullish()
      .transform((it) => it ?? undefined),
  }),
  z.object({ type: z.literal("deltaPercent"), factor: z.number() }),
  z.object({ type: z.literal("metric"), metric: z.string() }),
  z.object({ type: z.literal("text"), text: z.string() }),
]);
