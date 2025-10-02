import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getRepoHistory } from "@/api/repoHistory.ts";

export function useRepoHistory(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["repoHistory", { repo }], queryFn: () => getRepoHistory(toValue(repo)) });
}
