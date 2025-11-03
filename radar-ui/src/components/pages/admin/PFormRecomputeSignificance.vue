<script setup lang="ts">
import { postAdminRecomputeSignificance } from "@/api/adminRecomputeSignificance.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlCol from "@/components/CControlCol.vue";
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
  <CControl>
    <CControlCol>
      <PSelectRepo v-model="repo" />

      <CButton class="w-fit" :disabled="admin.token === undefined || repo === undefined" @click="onClick()">
        Recompute significance of all commits
      </CButton>
    </CControlCol>
  </CControl>
</template>
