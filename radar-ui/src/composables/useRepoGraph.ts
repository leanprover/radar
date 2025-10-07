import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getRepoGraph } from "@/api/repoGraph.ts";

export function useRepoGraph(
  repo: MaybeRefOrGetter<string>,
  m: MaybeRefOrGetter<string[]>,
  n: MaybeRefOrGetter<number>,
) {
  return useQuery({
    queryKey: ["repoGraph", { repo, m, n }],
    queryFn: () => getRepoGraph(toValue(repo), toValue(m), toValue(n)),
  });
}
