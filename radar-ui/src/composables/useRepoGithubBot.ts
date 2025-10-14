import { getRepoGithubBot } from "@/api/repoGithubBot.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";

export function useRepoGithubBot(repo: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["repoGithubBot", { repo }],
    queryFn: () => getRepoGithubBot(toValue(repo)),
    refetchInterval: 30 * 1000,
  });
}
