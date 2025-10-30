<script setup lang="ts">
import { postAdminRecomputeSignificance } from "@/api/adminRecomputeSignificance.ts";
import CButton from "@/components/CButton.vue";
import PSelectRepo from "@/components/pages/admin/PSelectRepo.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { ref } from "vue";

const admin = useAdminStore();
const repo = ref<string>();

async function onClick() {
  if (admin.token === undefined) return;
  if (repo.value === undefined) return;
  await postAdminRecomputeSignificance(admin.token, repo.value);
}
</script>

<template>
  <div class="bg-background-alt flex flex-col items-start gap-2 p-1">
    <PSelectRepo v-model="repo" />
    <CButton :disabled="admin.token === undefined || repo === undefined" @click="onClick()">
      Recompute significance of all commits
    </CButton>
  </div>
</template>
