<script setup lang="ts">
import { postAdminResetRssBotState } from "@/api/adminResetRssBotState.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlCol from "@/components/CControlCol.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";

const { repo, chash } = defineProps<{ repo: string; chash: string }>();

const admin = useAdminStore();

async function onClick() {
  if (admin.token === undefined) return;
  await postAdminResetRssBotState(admin.token, repo, chash);
}
</script>

<template>
  <CControl class="max-w-[80ch]">
    <CControlCol>
      <div>
        <CButton @click="onClick()"> Reset RSS bot state </CButton>
      </div>
      <details>
        <summary>Explanation</summary>
        <div class="mt-2">Make the RSS bot reconsider this commit, posting it (again) if it is significant.</div>
      </details>
    </CControlCol>
  </CControl>
</template>
