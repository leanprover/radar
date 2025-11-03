<script setup lang="ts">
import { formatZoned, formatZonedDate, formatZonedTime, instantToZoned } from "@/lib/format.ts";
import { Temporal } from "temporal-polyfill";
import { computed } from "vue";

const { when, date = true } = defineProps<{ when: Temporal.Instant; date?: boolean }>();
const whenTime = computed(() => instantToZoned(when));
</script>

<template>
  <span v-if="date">{{ formatZoned(whenTime) }}</span>
  <span v-else :title="formatZonedDate(whenTime)">{{ formatZonedTime(whenTime) }}</span>
</template>
