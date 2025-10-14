import { enc, fetchJson, JsonCommit } from "@/api/utils.ts";
import * as z from "zod";

export type JsonMetric = z.infer<typeof JsonMetric>;
export const JsonMetric = z.object({
  metric: z.string(),
  direction: z.union([z.literal(-1), z.literal(0), z.literal(1)]),
  measurements: z.number().nullable().array(),
});

export type JsonGet = z.infer<typeof JsonGet>;
export const JsonGet = z.object({
  commits: JsonCommit.array(),
  metrics: JsonMetric.array(),
});

export async function getRepoGraph(repo: string, m: string[], n: number) {
  const queryParams = new URLSearchParams();

  queryParams.set("n", n.toFixed());
  for (const metric of m) queryParams.append("m", metric);

  return await fetchJson(JsonGet, `/repos/${enc(repo)}/graph/`, queryParams);
}
