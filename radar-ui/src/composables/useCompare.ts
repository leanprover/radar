import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getCompare } from "@/api/compare.ts";

export function useCompare(
  repo: MaybeRefOrGetter<string>,
  first: MaybeRefOrGetter<string>,
  second: MaybeRefOrGetter<string>,
) {
  return useQuery({
    queryKey: ["compare", { repo, first, second }],
    queryFn: () => getCompare(toValue(repo), toValue(first), toValue(second)),
  });
}
