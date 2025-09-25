import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { getCommit } from "@/api/commit.ts";
import { toValue } from "vue";

export function useCommit(repo: MaybeRefOrGetter<string>, chash: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["commit", { repo, chash }],
    queryFn: () => getCommit(toValue(repo), toValue(chash)),
  });
}
