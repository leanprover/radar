import * as z from "zod";
import { fetchJson, Timestamp } from "@/api/utils.ts";

const JsonRunner = z.object({
  name: z.string(),
  lastSeen: Timestamp.nullable(),
  connected: z.boolean(),
});

const JsonGet = z.object({
  runners: z.array(JsonRunner),
});

export async function getQueue() {
  return await fetchJson("/queue", JsonGet);
}
