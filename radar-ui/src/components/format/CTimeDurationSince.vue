<script setup lang="ts">
import { formatDuration, formatZoned, instantToZoned } from "@/lib/format.ts";
import { useNow } from "@vueuse/core";
import { Temporal, toTemporalInstant } from "temporal-polyfill";
import { computed } from "vue";

const { start } = defineProps<{ start: Temporal.Instant }>();
const startTime = computed(() => instantToZoned(start));

const now = useNow();
</script>

<template>
  <span :title="`since ${formatZoned(startTime)}`">{{ formatDuration(start.until(toTemporalInstant.call(now))) }}</span>
</template>
