import { enc, fetchJson, JsonOutputLine, NotFoundError, Timestamp } from "@/api/utils.ts";
import * as z from "zod";

const JsonOutputLineBatch = z.object({
  lines: JsonOutputLine.array(),
  start: z.int(),
});

const JsonActiveRun = z.object({
  benchChash: z.string(),
  startTime: Timestamp,
  lines: JsonOutputLineBatch,
});

const JsonGet = z.object({
  runner: z.string(),
  script: z.string(),
  activeRun: JsonActiveRun.nullish().transform((it) => it ?? undefined),
});

export async function getQueueRun(repo: string, chash: string, run: string) {
  try {
    return await fetchJson(JsonGet, `/queue/runs/${enc(repo)}/${enc(chash)}/${enc(run)}/`);
  } catch (e) {
    if (e instanceof NotFoundError) return "not found";
    throw e;
  }
}
