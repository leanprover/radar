<script setup lang="ts">
import { useAdminStore } from "@/stores/useAdminStore.ts";
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";

const admin = useAdminStore();
</script>

<template>
  <CSection>
    <CSectionTitle>Admin token</CSectionTitle>
    <div class="bg-background-alt w-fit p-1">
      Current token:
      <input v-model="admin.token" type="text" placeholder="<none>" class="bg-background w-[70ch] px-1" />
    </div>
    <div>To get a token, ask your local admin.</div>
  </CSection>
</template>
