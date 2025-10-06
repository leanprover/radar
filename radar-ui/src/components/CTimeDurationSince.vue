<script setup lang="ts">
import { Temporal, toTemporalInstant } from "temporal-polyfill";
import { useNow } from "@vueuse/core";
import { computed } from "vue";
import { formatDuration, formatZoned, instantToZoned } from "@/lib/format.ts";

const { start } = defineProps<{ start: Temporal.Instant }>();
const startTime = computed(() => instantToZoned(start));

const now = useNow();
</script>

<template>
  <span :title="`since ${formatZoned(startTime)}`">{{ formatDuration(start.until(toTemporalInstant.call(now))) }}</span>
</template>
