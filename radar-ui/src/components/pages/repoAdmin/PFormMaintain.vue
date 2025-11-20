<script setup lang="ts">
import { postAdminMaintain } from "@/api/adminMaintain.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlRow from "@/components/CControlRow.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";

const { repo } = defineProps<{ repo: string }>();
const admin = useAdminStore();

async function onClick(aggressive: boolean) {
  if (admin.token === undefined) return;
  await postAdminMaintain(admin.token, repo, aggressive);
}
</script>

<template>
  <CControl>
    <CControlRow>
      <CButton class="w-fit" :disabled="admin.token === undefined || repo === undefined" @click="onClick(false)">
        Perform normal maintenance
      </CButton>

      <CButton class="w-fit" :disabled="admin.token === undefined || repo === undefined" @click="onClick(true)">
        Perform aggressive maintenance
      </CButton>
    </CControlRow>
  </CControl>
</template>
