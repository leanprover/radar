import { getCommit } from "@/api/commit.ts";
import { QueryClient, useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";

export function useCommit(repo: MaybeRefOrGetter<string>, chash: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["commit", { repo, chash }],
    queryFn: () => getCommit(toValue(repo), toValue(chash)),
  });
}

export async function invalidateCommit(queryClient: QueryClient, repo: string, chash: string) {
  await queryClient.invalidateQueries({ queryKey: ["commit", { repo, chash }] });
}
