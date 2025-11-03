<script setup lang="ts">
import { formatDuration, formatZonedRange, instantToZoned } from "@/lib/format.ts";
import { Temporal } from "temporal-polyfill";
import { computed } from "vue";

const { start, end } = defineProps<{ start: Temporal.Instant; end: Temporal.Instant }>();
const startTime = computed(() => instantToZoned(start));
const endTime = computed(() => instantToZoned(end));
</script>

<template>
  <span :title="formatZonedRange(startTime, endTime)">{{ formatDuration(start.until(end)) }}</span>
</template>
