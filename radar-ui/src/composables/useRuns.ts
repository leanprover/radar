import { toValue } from "vue";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { useQuery } from "@tanstack/vue-query";
import { getRuns } from "@/api/runs.ts";

export function useRuns(repo: MaybeRefOrGetter<string>, chash: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["runs", { repo, chash }],
    queryFn: () => getRuns(toValue(repo), toValue(chash)),
  });
}
