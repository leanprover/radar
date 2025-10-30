<script setup lang="ts">
import { useAdminStore } from "@/stores/useAdminStore.ts";

const admin = useAdminStore();
</script>

<template>
  <div class="bg-background-alt p-1">
    Current token:
    <input v-model="admin.token" type="text" placeholder="<none>" class="bg-background w-[70ch] px-1" />
  </div>
</template>
