import { getRepoMetrics } from "@/api/repoMetrics.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";

export function useRepoMetrics(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["repoMetrics", { repo }], queryFn: () => getRepoMetrics(toValue(repo)) });
}
