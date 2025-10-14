import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getRepoMetrics } from "@/api/repoMetrics.ts";

export function useRepoMetrics(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["repoMetrics", { repo }], queryFn: () => getRepoMetrics(toValue(repo)) });
}
