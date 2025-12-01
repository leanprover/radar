<script setup lang="ts">
import { formatDurationLikeTimestamp } from "@/lib/format.ts";
import { Temporal } from "temporal-polyfill";

export interface Line {
  source: number;
  time: Temporal.Instant;
  line: string;
}

const { startTime, startLine, lines } = defineProps<{
  startTime: Temporal.Instant;
  startLine: number;
  lines: Line[];
}>();
</script>

<template>
  <div>Lines: {{ startLine + lines.length }}</div>
  <div class="flex flex-col">
    <span
      v-for="(line, index) in lines"
      :key="index"
      :class="[
        'whitespace-pre-wrap',
        {
          'text-red': line.source === 1,
          'text-blue': line.source === 2,
          'font-bold': line.source === 3,
          'text-red font-bold': line.source === 4,
        },
      ]"
      >[{{ formatDurationLikeTimestamp(startTime.until(line.time)) }}] {{ line.line }}{{ "\n" }}</span
    >
  </div>
</template>
