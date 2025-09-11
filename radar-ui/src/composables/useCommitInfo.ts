import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { getReposRepoCommitsChash } from "@/api/reposRepoCommitsChash.ts";
import { toValue } from "vue";

// See also https://github.com/TanStack/query/issues/5418
export function useCommitInfo(repo: MaybeRefOrGetter<string>, chash: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["repos", repo, "commits", chash],
    queryFn: () => getReposRepoCommitsChash(toValue(repo), toValue(chash)),
  });
}
