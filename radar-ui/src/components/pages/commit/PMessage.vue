<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import PMessageSegment from "@/components/pages/commit/PMessageSegment.vue";

const { segments } = defineProps<{ segments: JsonMessageSegment[] }>();
</script>

<template>
  <div>
    <PMessageSegment v-for="(segment, i) in segments" :key="i" :segment="segment" />
  </div>
</template>
