import * as z from "zod";
import { enc, fetchJson, Timestamp } from "@/api/utils.ts";

const JsonRun = z.object({
  name: z.string(),
  script: z.string(),
  runner: z.string(),
  benchChash: z.string(),
  startTime: Timestamp,
  endTime: Timestamp,
  exitCode: z.int(),
});

const JsonGet = z.object({
  runs: z.array(JsonRun),
});

export async function getRuns(repo: string, chash: string) {
  return await fetchJson(JsonGet, `/runs/${enc(repo)}/${enc(chash)}/`);
}
