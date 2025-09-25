<script setup lang="ts">
import { Temporal } from "temporal-polyfill";
import { formatDuration, formatInstant } from "@/lib/utils.ts";

const { start, end } = defineProps<{ start: Temporal.Instant; end: Temporal.Instant }>();
</script>

<template>
  <span :title="formatInstant(start) + ' until ' + formatInstant(end)">{{ formatDuration(start.until(end)) }}</span>
</template>
