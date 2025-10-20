import { getCompare } from "@/api/compare.ts";
import { QueryClient, useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";

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

export async function invalidateCompare(queryClient: QueryClient, repo: string, first: string, second: string) {
  await queryClient.invalidateQueries({ queryKey: ["compare", { repo, first, second }] });
}
