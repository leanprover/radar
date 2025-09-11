import * as z from "zod";
import { fetchJson } from "@/api/utils.ts";

const JsonCommit = z.object({
  chash: z.string(),
  title: z.string(),
  author: z.string(),
  committer: z.string(),
  committerTime: z.number(),
});

const JsonGet = z.object({
  commits: z.array(JsonCommit),
  nextAt: z.int().nullable(),
});

export async function getRepoNameHistory(name: string, params?: { n?: number; at?: number }) {
  return await fetchJson(JsonGet, `/repos/${encodeURIComponent(name)}/history`, params);
}
