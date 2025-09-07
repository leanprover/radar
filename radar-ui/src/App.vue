<script setup lang="ts">
import CNavbar from "@/components/CNavbar.vue";
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <CNavbar />
    <div class="p-2">
      <RouterView />
    </div>
  </div>
</template>
