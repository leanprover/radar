<script setup lang="ts">
import CLink from "@/components/link/CLink.vue";

const { repo } = defineProps<{ repo: string }>();
</script>

<template>
  <CLink
    ><RouterLink :to="{ name: '/repos.[repo]', params: { repo } }">{{ repo }}</RouterLink></CLink
  >
</template>
