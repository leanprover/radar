<script setup lang="ts">
const { marker = "-" } = defineProps<{ marker?: string }>();
</script>

<template>
  <div class="flex gap-2">
    <div>{{ marker }}</div>
    <slot />
  </div>
</template>
