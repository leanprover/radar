import { computed, toValue } from "vue";
import { useRepos } from "@/composables/useRepos.ts";
import type { MaybeRefOrGetter } from "@vueuse/core";

export function useRepo(repo: MaybeRefOrGetter<string>) {
  const { data } = useRepos();

  return computed(() => {
    const repoValue = toValue(repo);
    return data.value?.repos.find((it) => it.name === repoValue);
  });
}
