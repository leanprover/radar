<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import PDetailsMessageSegment from "@/components/pages/commit/PDetailsMessageSegment.vue";

const { repo, chash, segments } = defineProps<{ repo: string; chash: string; segments: JsonMessageSegment[] }>();
</script>

<template>
  <div>
    <PDetailsMessageSegment v-for="(segment, i) in segments" :key="i" :repo :chash :segment="segment" />
  </div>
</template>
