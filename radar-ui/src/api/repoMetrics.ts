import { enc, fetchJson } from "@/api/utils.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";
import * as z from "zod";

const JsonMetric = z.object({
  metric: z.string(),
  unit: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
});

const JsonGet = z.object({
  metrics: JsonMetric.array(),
});

export async function getRepoMetrics(repo: string) {
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/metrics/`);
}

export function useRepoMetrics(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["repoMetrics", { repo }], queryFn: () => getRepoMetrics(toValue(repo)) });
}
