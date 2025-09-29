import * as z from "zod";
import { enc, fetchJson, JsonRun, Timestamp } from "@/api/utils.ts";

const JsonPersonIdent = z.object({
  name: z.string(),
  email: z.string(),
  time: Timestamp,
  offset: z.int(),
});

const JsonLinkedCommit = z.object({
  chash: z.string(),
  title: z.string(),
  tracked: z.boolean(),
});

const JsonGet = z.object({
  chash: z.string(),
  author: JsonPersonIdent,
  committer: JsonPersonIdent,
  title: z.string(),
  body: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  parents: z.array(JsonLinkedCommit),
  children: z.array(JsonLinkedCommit),
  runs: z.array(JsonRun),
});

export async function getCommit(repo: string, chash: string) {
  return await fetchJson(JsonGet, `/commits/${enc(repo)}/${enc(chash)}/`);
}
