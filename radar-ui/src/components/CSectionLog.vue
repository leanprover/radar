<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import { formatZonedTime, instantToZoned } from "@/lib/format.ts";
import { Temporal } from "temporal-polyfill";

export interface Line {
  source: number;
  time: Temporal.Instant;
  line: string;
}

const { lines = undefined, start = 0 } = defineProps<{ lines?: Line[]; start?: number }>();
</script>

<template>
  <CSection title="Logs">
    <div v-if="lines === undefined">No logs available.</div>
    <template v-else>
      <div>Lines: {{ start + lines.length }}</div>
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
          >[{{ formatZonedTime(instantToZoned(line.time)) }}] {{ line.line }}{{ "\n" }}</span
        >
      </div>
    </template>
  </CSection>
</template>
