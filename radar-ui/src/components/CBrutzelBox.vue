<script setup lang="ts">
const { shadow = false } = defineProps<{ shadow?: boolean }>();
</script>

<template>
  <div class="flex flex-col">
    <div
      :class="{
        'flex min-h-0 flex-col bg-white text-black': true,
        'shadow-[inset_0_-20px_10px_-20px_rgb(0_0_0/50%)]': shadow,
      }"
    >
      <slot />
    </div>
    <div class="hidden pt-[0.1em] text-center text-[0.6em] dark:block">Sorry for brutzeling your eye balls.</div>
  </div>
</template>
