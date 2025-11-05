<script setup lang="ts">
import CSectionTitle from "@/components/CSectionTitle.vue";

const { title, subtitle = undefined } = defineProps<{ title: string; subtitle?: string }>();
</script>

<template>
  <div class="border-background-alt flex flex-col gap-2 border-l-2 px-2">
    <CSectionTitle :title :subtitle />
    <slot />
  </div>
</template>
