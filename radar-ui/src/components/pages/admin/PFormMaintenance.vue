<script setup lang="ts">
import { postAdminMaintain } from "@/api/adminMaintain.ts";
import CButton from "@/components/CButton.vue";
import PSelectRepo from "@/components/pages/admin/PSelectRepo.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { ref } from "vue";

const admin = useAdminStore();
const repo = ref<string>();
const aggressive = ref(false);

async function onClick() {
  if (admin.token === undefined) return;
  if (repo.value === undefined) return;
  await postAdminMaintain(admin.token, repo.value, aggressive.value);
}
</script>

<template>
  <div class="bg-background-alt flex flex-col items-start gap-2 p-1">
    <PSelectRepo v-model="repo" />
    <label class="select-none">
      <input v-model="aggressive" type="checkbox" />
      Aggressive
    </label>
    <CButton :disabled="admin.token === undefined || repo === undefined" @click="onClick()">
      Perform {{ aggressive ? "aggressive" : null }} maintenance
    </CButton>
  </div>
</template>
