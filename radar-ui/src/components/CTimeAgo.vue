<script setup lang="ts">
import { Temporal } from "temporal-polyfill";
import { useTimeAgo } from "@vueuse/core";
import { computed } from "vue";
import { formatZoned, instantToZoned } from "@/lib/format.ts";

const { when } = defineProps<{ when: Temporal.Instant }>();
const whenTime = computed(() => instantToZoned(when));
</script>

<template>
  <span :title="formatZoned(whenTime)">{{ useTimeAgo(when.epochMilliseconds) }}</span>
</template>
