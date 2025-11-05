<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import PDetailsMessageSegment from "@/components/pages/commit/PDetailsMessageSegment.vue";

const { segments } = defineProps<{ segments: JsonMessageSegment[] }>();
</script>

<template>
  <div>
    <PDetailsMessageSegment v-for="(segment, i) in segments" :key="i" :segment="segment" />
  </div>
</template>
