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
  const queryParams = new URLSearchParams();
  if (params?.n !== undefined) queryParams.set("n", params.n.toFixed());
  if (params?.at !== undefined) queryParams.set("at", params.at.toFixed());
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/history`, queryParams);
}
