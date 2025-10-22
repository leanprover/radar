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
