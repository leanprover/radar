<script setup lang="ts">
import { postAdminRecomputeSignificance } from "@/api/adminRecomputeSignificance.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";

const { repo } = defineProps<{ repo: string }>();
const admin = useAdminStore();

async function onClick() {
  if (admin.token === undefined) return;
  await postAdminRecomputeSignificance(admin.token, repo);
}
</script>

<template>
  <CControl>
    <CButton class="w-fit" :disabled="admin.token === undefined" @click="onClick()">
      Recompute significance of all commits
    </CButton>
  </CControl>
</template>
