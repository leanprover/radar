import * as z from "zod";
import { fetchJson, Timestamp } from "@/api/utils.ts";

const JsonRunner = z.object({
  name: z.string(),
  lastSeen: Timestamp.nullable(),
  connected: z.boolean(),
});

const JsonRun = z.object({
  runner: z.string(),
  script: z.string(),
  state: z.union([z.literal("ready"), z.literal("running"), z.literal("success"), z.literal("error")]),
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
