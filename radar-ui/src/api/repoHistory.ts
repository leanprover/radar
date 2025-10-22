import { JsonCommit } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
import * as z from "zod";

const JsonGet = z.object({
  commits: JsonCommit.array(),
});

export async function getRepoHistory(repo: string, params?: { n?: number }) {
  const queryParams = new URLSearchParams();
  if (params?.n !== undefined) queryParams.set("n", params.n.toFixed());
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/history/`, queryParams);
}
