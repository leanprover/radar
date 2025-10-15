<script setup lang="ts">
import CNavbar from "@/components/CNavbar.vue";
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const screenHeight = computed(() => route.name === "/repos.[repo].graph");
</script>

<template>
  <div :class="{ 'flex flex-col': true, 'h-dvh': screenHeight }">
    <CNavbar />
    <div class="flex min-h-0 shrink grow flex-col gap-8 p-2 pt-4">
      <RouterView />
    </div>
  </div>
</template>
