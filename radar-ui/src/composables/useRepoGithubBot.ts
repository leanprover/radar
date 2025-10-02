import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getRepoGithubBot } from "@/api/repoGithubBot.ts";

export function useRepoGithubBot(repo: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["repoGithubBot", { repo }],
    queryFn: () => getRepoGithubBot(toValue(repo)),
    refetchInterval: 30 * 1000,
  });
}
