import * as z from "zod";
import { enc, fetchJson, Timestamp } from "@/api/utils.ts";

const JsonOutputLine = z
  .tuple([Timestamp, z.int(), z.string()])
  .transform(([time, source, line]) => ({ time, source, line }));

const JsonGet = z.object({
  runner: z.string(),
  script: z.string(),
  benchChash: z.string(),
  startTime: Timestamp,
  endTime: Timestamp,
  scriptStartTime: Timestamp.nullish().transform((it) => it ?? undefined),
  scriptEndTime: Timestamp.nullish().transform((it) => it ?? undefined),
  exitCode: z.int(),
  lines: JsonOutputLine.array(),
});

export async function getCommitRun(repo: string, chash: string, run: string) {
  return await fetchJson(JsonGet, `/commits/${enc(repo)}/${enc(chash)}/runs/${enc(run)}/`);
}
