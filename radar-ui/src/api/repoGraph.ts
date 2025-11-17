import { JsonCommit } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";
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

export const metricsLimit = 500;

export function useRepoGraph(
  repo: MaybeRefOrGetter<string>,
  m: MaybeRefOrGetter<string[]>,
  n: MaybeRefOrGetter<number>,
) {
  return useQuery({
    queryKey: ["repoGraph", { repo, m, n }],
    queryFn: () => getRepoGraph(toValue(repo), toValue(m), toValue(n)),
  });
}
