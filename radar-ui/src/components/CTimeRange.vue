<script setup lang="ts">
import { Temporal } from "temporal-polyfill";
import { computed } from "vue";
import { formatDuration, formatZonedRange, instantToZoned } from "@/lib/format.ts";

const { start, end } = defineProps<{ start: Temporal.Instant; end: Temporal.Instant }>();
const startTime = computed(() => instantToZoned(start));
const endTime = computed(() => instantToZoned(end));
</script>

<template>
  <span :title="formatDuration(start.until(end))">{{ formatZonedRange(startTime, endTime) }}</span>
</template>
