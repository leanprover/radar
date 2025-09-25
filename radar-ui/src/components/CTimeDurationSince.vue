<script setup lang="ts">
import { Temporal, toTemporalInstant } from "temporal-polyfill";
import { formatDuration, formatInstant } from "@/lib/utils.ts";
import { useNow } from "@vueuse/core";

const { start } = defineProps<{ start: Temporal.Instant }>();
const now = useNow();
</script>

<template>
  <span :title="'since ' + formatInstant(start)">{{ formatDuration(start.until(toTemporalInstant.call(now))) }}</span>
</template>
