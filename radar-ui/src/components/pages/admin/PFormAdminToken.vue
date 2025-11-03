<script setup lang="ts">
import CControl from "@/components/CControl.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";

const admin = useAdminStore();
</script>

<template>
  <CControl>
    <label>
      Current token:
      <input v-model="admin.token" type="text" placeholder="<none>" class="bg-background w-[70ch] px-1" />
    </label>
  </CControl>
</template>
