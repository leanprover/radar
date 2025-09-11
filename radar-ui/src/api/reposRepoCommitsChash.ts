import * as z from "zod";
import { enc, fetchJson, Timestamp } from "@/api/utils.ts";

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
  body: z.string().nullable(),
  parents: z.array(JsonLinkedCommit),
  children: z.array(JsonLinkedCommit),
});

export async function getReposRepoCommitsChash(repo: string, chash: string) {
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/commits/${enc(chash)}`);
}
