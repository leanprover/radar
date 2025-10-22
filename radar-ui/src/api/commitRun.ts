import { JsonOutputLine, Timestamp } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
import * as z from "zod";

const JsonGet = z.object({
  runner: z.string(),
  script: z.string(),
  benchChash: z.string(),
  startTime: Timestamp,
  endTime: Timestamp,
  scriptStartTime: Timestamp.nullish().transform((it) => it ?? undefined),
  scriptEndTime: Timestamp.nullish().transform((it) => it ?? undefined),
  exitCode: z.int(),
  lines: JsonOutputLine.array()
    .nullish()
    .transform((lines) => lines ?? undefined),
});

export async function getCommitRun(repo: string, chash: string, run: string) {
  return await fetchJson(JsonGet, `/commits/${enc(repo)}/${enc(chash)}/runs/${enc(run)}/`);
}
