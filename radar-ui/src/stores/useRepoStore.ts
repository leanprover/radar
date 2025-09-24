import { defineStore } from "pinia";
import { useStorage } from "@vueuse/core";
import { ref, watch } from "vue";

export const useRepoStore = defineStore("repo", () => {
  const stored = useStorage<string | undefined>("radar/selected-repo", undefined);

  const selected = ref<string | undefined>(stored.value);

  watch(selected, () => {
    stored.value = selected.value;
  });

  return { selected };
});
