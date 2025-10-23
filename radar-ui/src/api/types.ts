import { Temporal } from "temporal-polyfill";
import * as z from "zod";

export const Timestamp = z.number().transform((it) => Temporal.Instant.fromEpochMilliseconds(Math.round(it * 1000)));

export type Direction = -1 | 0 | 1;
export const Direction = z.union([z.literal(-1), z.literal(-0), z.literal(1)]);

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
  | { type: "delta"; amount: number; unit?: string; direction: Direction }
  | { type: "deltaPercent"; factor: number; direction: Direction }
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
    direction: Direction,
  }),
  z.object({ type: z.literal("deltaPercent"), factor: z.number(), direction: Direction }),
  z.object({ type: z.literal("metric"), metric: z.string() }),
  z.object({ type: z.literal("text"), text: z.string() }),
]);

export const JsonMetricSignificance = z.object({
  major: z.boolean(),
  message: JsonMessageSegment.array(),
});

export const JsonMetricComparison = z.object({
  metric: z.string(),
  first: z
    .number()
    .nullish()
    .transform((it) => it ?? undefined),
  second: z
    .number()
    .nullish()
    .transform((it) => it ?? undefined),
  firstSource: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  secondSource: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  unit: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  direction: Direction,
  significance: JsonMetricSignificance.nullish().transform((it) => it ?? undefined),
});
