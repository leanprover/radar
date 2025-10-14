import { getCommitRun } from "@/api/commitRun.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";

export function useCommitRun(
  repo: MaybeRefOrGetter<string>,
  chash: MaybeRefOrGetter<string>,
  run: MaybeRefOrGetter<string>,
) {
  return useQuery({
    queryKey: ["commitRun", { repo, chash, run }],
    queryFn: () => getCommitRun(toValue(repo), toValue(chash), toValue(run)),
  });
}
