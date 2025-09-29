import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { getQueueRun } from "@/api/queueRun.ts";
import { toValue } from "vue";

export function useQueueRun(
  repo: MaybeRefOrGetter<string>,
  chash: MaybeRefOrGetter<string>,
  run: MaybeRefOrGetter<string>,
) {
  return useQuery({
    queryKey: ["queueRun", { repo, chash, run }],
    queryFn: () => getQueueRun(toValue(repo), toValue(chash), toValue(run)),
    refetchInterval: 1000,
  });
}
