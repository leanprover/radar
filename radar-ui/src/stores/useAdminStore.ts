import { useStorage } from "@vueuse/core";
import { defineStore } from "pinia";

export const useAdminStore = defineStore("admin", () => {
  const token = useStorage<string | undefined>("radar/admin-token", undefined);
  return { token };
});
