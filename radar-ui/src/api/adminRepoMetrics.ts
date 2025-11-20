import { enc, fetchJson } from "@/api/utils.ts";
import { QueryClient, useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";
import * as z from "zod";

export interface JsonMetric {
  metric: string;
  unit?: string;
  appearsInLatestCommit: boolean;
  appearsInHistoricalCommits: number;
}
const JsonMetric = z.object({
  metric: z.string(),
  unit: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  appearsInLatestCommit: z.boolean(),
  appearsInHistoricalCommits: z.int(),
});

const JsonGet = z.object({
  metrics: JsonMetric.array(),
});

export async function getAdminRepoMetrics(repo: string) {
  return await fetchJson(JsonGet, `/admin/repos/${enc(repo)}/metrics/`);
}

export function useAdminRepoMetrics(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["adminRepoMetrics", { repo }], queryFn: () => getAdminRepoMetrics(toValue(repo)) });
}

export async function invalidateAdminRepoMetrics(queryClient: QueryClient, repo: string) {
  await queryClient.invalidateQueries({ queryKey: ["adminRepoMetrics", { repo }] });
}
