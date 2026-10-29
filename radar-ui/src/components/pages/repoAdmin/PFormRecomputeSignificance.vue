<script setup lang="ts">
import { postAdminRecomputeSignificance } from "@/api/adminRecomputeSignificance.ts";
import CActionButton from "@/components/CActionButton.vue";
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
    <CActionButton class="w-fit" :disabled="admin.token === undefined" :action="onClick">
      Recompute significance of all commits
    </CActionButton>
  </CControl>
</template>
