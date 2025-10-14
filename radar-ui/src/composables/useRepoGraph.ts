import { getRepoGraph } from "@/api/repoGraph.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";

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
