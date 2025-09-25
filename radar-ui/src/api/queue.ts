import * as z from "zod";
import { fetchJson, Timestamp } from "@/api/utils.ts";

export type JsonActiveRun = z.infer<typeof JsonActiveRun>;
const JsonActiveRun = z.object({
  repo: z.string(),
  chash: z.string(),
  name: z.string(),
  startTime: Timestamp,
});

export type JsonRunner = z.infer<typeof JsonRunner>;
const JsonRunner = z.object({
  name: z.string(),
  connected: z.boolean(),
  lastSeen: Timestamp.nullish().transform((it) => it ?? undefined),
  activeRun: JsonActiveRun.nullish().transform((it) => it ?? undefined),
});

export type JsonRunResult = z.infer<typeof JsonRunResult>;
const JsonRunResult = z.object({
  startTime: Timestamp,
  endTime: Timestamp,
  exitCode: z.int(),
});

export type JsonRun = z.infer<typeof JsonRun>;
const JsonRun = z.object({
  name: z.string(),
  script: z.string(),
  runner: z.string(),
  result: JsonRunResult.nullish().transform((it) => it ?? undefined),
});

export type JsonTask = z.infer<typeof JsonTask>;
const JsonTask = z.object({
  repo: z.string(),
  chash: z.string(),
  title: z.string(),
  runs: z.array(JsonRun),
});

export type JsonGet = z.infer<typeof JsonGet>;
const JsonGet = z.object({
  runners: z.array(JsonRunner),
  tasks: z.array(JsonTask),
});

export async function getQueue(): Promise<JsonGet> {
  return await fetchJson(JsonGet, "/queue/");
}
