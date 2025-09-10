import { defineStore } from "pinia";
import { ref } from "vue";

export const useRepoStore = defineStore("repo", () => {
  const selected = ref<string>();
  return { selected };
});
