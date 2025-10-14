import { enc, fetchJson, JsonCommit, JsonRun } from "@/api/utils.ts";
import * as z from "zod";

const JsonLinkedCommit = z.object({
  chash: z.string(),
  title: z.string(),
  tracked: z.boolean(),
});

const JsonGet = z.object({
  commit: JsonCommit,
  parents: z.array(JsonLinkedCommit),
  children: z.array(JsonLinkedCommit),
  runs: z.array(JsonRun),
});

export async function getCommit(repo: string, chash: string) {
  return await fetchJson(JsonGet, `/commits/${enc(repo)}/${enc(chash)}/`);
}
