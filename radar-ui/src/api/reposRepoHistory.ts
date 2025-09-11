import * as z from "zod";
import { enc, fetchJson, Timestamp } from "@/api/utils.ts";

const JsonCommit = z.object({
  chash: z.string(),
  title: z.string(),
  author: z.string(),
  committerTime: Timestamp,
});

const JsonGet = z.object({
  commits: z.array(JsonCommit),
  nextAt: z.int().nullable(),
});

export async function getReposRepoHistory(repo: string, params?: { n?: number; at?: number }) {
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/history`, params);
}
