import * as z from "zod";
import { enc, fetchJson } from "@/api/utils.ts";

const JsonPersonIdent = z.object({
  name: z.string(),
  email: z.string(),
  time: z.number(),
  offset: z.int(),
});

const JsonGet = z.object({
  chash: z.string(),
  author: JsonPersonIdent,
  committer: JsonPersonIdent,
  title: z.string(),
  body: z.string(),
  parents: z.string().array(),
  children: z.string().array(),
});

export async function getReposNameCommitsChash(name: string, chash: string) {
  return await fetchJson(JsonGet, `/repos/${enc(name)}/commits/${enc(chash)}`);
}
