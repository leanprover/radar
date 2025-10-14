import { getRepoHistory } from "@/api/repoHistory.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";

export function useRepoHistory(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["repoHistory", { repo }], queryFn: () => getRepoHistory(toValue(repo)) });
}
