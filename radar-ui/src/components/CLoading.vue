<script setup lang="ts">
const { error } = defineProps<{ error: Error | null | undefined }>();
</script>

<template>
  <div v-if="error" :title="error.message" class="text-red font-bold">Error</div>
  <div v-else class="text-foreground-alt">Loading...</div>
</template>
