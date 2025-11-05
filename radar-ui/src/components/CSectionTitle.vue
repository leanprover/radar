<script setup lang="ts">
const { title, subtitle = undefined } = defineProps<{ title: string; subtitle?: string }>();
</script>

<template>
  <div v-if="subtitle" class="flex flex-col">
    <h2 class="text-base font-bold">{{ title }}</h2>
    <div class="text-foreground-alt text-xs">{{ subtitle }}</div>
  </div>
  <h2 v-else class="text-base font-bold">{{ title }}</h2>
</template>
