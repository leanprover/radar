import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getReposRepoHistory } from "@/api/reposRepoHistory.ts";

export function useRepoHistory(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["repos", repo, "history"], queryFn: () => getReposRepoHistory(toValue(repo)) });
}
