import { JsonCommit, JsonRun, Timestamp } from "@/api/types.ts";
import { fetchJson } from "@/api/utils.ts";
import * as z from "zod";

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
});

export type JsonTask = z.infer<typeof JsonTask>;
const JsonTask = z.object({
  repo: z.string(),
  commit: JsonCommit,
  runs: JsonRun.array(),
});

export type JsonGet = z.infer<typeof JsonGet>;
const JsonGet = z.object({
  runners: z.array(JsonRunner),
  tasks: z.array(JsonTask),
});

export async function getQueue(): Promise<JsonGet> {
  return await fetchJson(JsonGet, "/queue/");
}
