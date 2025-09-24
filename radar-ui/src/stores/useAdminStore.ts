import { defineStore } from "pinia";
import { useStorage } from "@vueuse/core";

export const useAdminStore = defineStore("admin", () => {
  const token = useStorage<string | undefined>("radar/admin-token", undefined);
  return { token };
});
