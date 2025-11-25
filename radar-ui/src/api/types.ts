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

export interface JsonCommitIdent {
  name: string;
  email: string;
  time: Temporal.Instant;
  offset: number;
}
export const JsonCommitIdent = z.object({
  name: z.string(),
  email: z.string(),
  time: Timestamp,
  offset: z.int(),
});

export interface JsonCommit {
  chash: string;
  author: JsonCommitIdent;
  committer: JsonCommitIdent;
  title: string;
  body?: string;
}
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

export type JsonMessageGoodness = "GOOD" | "NEUTRAL" | "BAD";
export const JsonMessageGoodness = z.union([z.literal("GOOD"), z.literal("NEUTRAL"), z.literal("BAD")]);

export type JsonMessageSegment =
  | { type: "delta"; amount: number; unit?: string; goodness: JsonMessageGoodness }
  | { type: "deltaPercent"; factor: number; goodness: JsonMessageGoodness }
  | { type: "exitCode"; exitCode: number; goodness: JsonMessageGoodness }
  | { type: "metric"; metric: string }
  | { type: "run"; run: string }
  | { type: "text"; text: string };
export const JsonMessageSegment = z.discriminatedUnion("type", [
  z.object({
    type: z.literal("delta"),
    amount: z.number(),
    unit: z
      .string()
      .nullish()
      .transform((it) => it ?? undefined),
    goodness: JsonMessageGoodness,
  }),
  z.object({ type: z.literal("deltaPercent"), factor: z.number(), goodness: JsonMessageGoodness }),
  z.object({ type: z.literal("exitCode"), exitCode: z.int(), goodness: JsonMessageGoodness }),
  z.object({ type: z.literal("metric"), metric: z.string() }),
  z.object({ type: z.literal("run"), run: z.string() }),
  z.object({ type: z.literal("text"), text: z.string() }),
]);

export interface JsonMessage {
  goodness: JsonMessageGoodness;
  segments: JsonMessageSegment[];
}
export const JsonMessage = z.object({
  goodness: JsonMessageGoodness,
  segments: JsonMessageSegment.array(),
});

export interface JsonSignificance {
  major: boolean;
  message: JsonMessage;
}
export const JsonSignificance = z.object({
  major: z.boolean(),
  message: JsonMessage,
});

export interface JsonRunAnalysis {
  name: string;
  script: string;
  runner: string;
  exitCode: number;
  significance?: JsonSignificance;
}
export const JsonRunAnalysis = z.object({
  name: z.string(),
  script: z.string(),
  runner: z.string(),
  exitCode: z.int(),
  significance: JsonSignificance.nullish().transform((it) => it ?? undefined),
});

export interface JsonMetricComparison {
  metric: string;
  first?: number;
  second?: number;
  firstSource?: string;
  secondSource?: string;
  unit?: string;
  direction: Direction;
  significance?: JsonSignificance;
}
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
  significance: JsonSignificance.nullish().transform((it) => it ?? undefined),
});

export interface JsonCommitComparison {
  significant: boolean;
  runs: JsonRunAnalysis[];
  metrics: JsonMetricComparison[];
}
export const JsonCommitComparison = z.object({
  significant: z.boolean(),
  runs: JsonRunAnalysis.array(),
  metrics: JsonMetricComparison.array(),
});
