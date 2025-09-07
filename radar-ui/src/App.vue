<script setup lang="ts">
import CNavbar from "@/components/CNavbar.vue";
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <CNavbar />
    <div class="flex grow flex-col p-6">
      <RouterView />
    </div>
  </div>
</template>
