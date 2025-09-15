import * as z from "zod";
import { fetchJson, Timestamp } from "@/api/utils.ts";

const JsonActiveRun = z.object({
  repo: z.string(),
  chash: z.string(),
  script: z.string(),
});

const JsonRunner = z.object({
  name: z.string(),
  connected: z.boolean(),
  lastSeen: Timestamp.nullable(),
  activeRun: JsonActiveRun.nullable(),
});

const JsonRun = z.object({
  runner: z.string(),
  script: z.string(),
  exitCode: z.int().nullable(),
});

const JsonTask = z.object({
  repo: z.string(),
  chash: z.string(),
  title: z.string(),
  runs: z.array(JsonRun),
});

const JsonGet = z.object({
  runners: z.array(JsonRunner),
  tasks: z.array(JsonTask),
});

export async function getQueue() {
  return await fetchJson(JsonGet, "/queue");
}
