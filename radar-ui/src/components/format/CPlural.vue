<script setup lang="ts">
const { n, plural = undefined } = defineProps<{ n: boolean | number; plural?: string }>();
</script>

<template>
  <slot v-if="n === false || n === 1 || n === -1" />
  <template v-else-if="plural === undefined"><slot />s</template>
  <template v-else>{{ plural }}</template>
</template>
