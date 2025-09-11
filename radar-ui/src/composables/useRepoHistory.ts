import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getRepoNameHistory } from "@/api/repoNameHistory.ts";

export function useRepoHistory(repo: MaybeRefOrGetter<string>) {
  return useQuery({ queryKey: ["repos", repo, "history"], queryFn: () => getRepoNameHistory(toValue(repo)) });
}
