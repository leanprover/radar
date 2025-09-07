import { computed, type Ref, toValue } from "vue";
import { type JsonRepo } from "@/api.ts";
import { useRepos } from "@/composables/useRepos.ts";
import type { MaybeRefOrGetter } from "@vueuse/core";

export function useRepoInfo(repo: MaybeRefOrGetter<string>): Ref<JsonRepo | undefined> {
  const { data } = useRepos();

  return computed(() => {
    const repoValue = toValue(repo);
    return data.value?.find((it) => it.name === repoValue);
  });
}
