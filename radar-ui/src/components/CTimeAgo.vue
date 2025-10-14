<script setup lang="ts">
import { formatZoned, instantToZoned } from "@/lib/format.ts";
import { useTimeAgo } from "@vueuse/core";
import { Temporal } from "temporal-polyfill";
import { computed } from "vue";

const { when } = defineProps<{ when: Temporal.Instant }>();
const whenTime = computed(() => instantToZoned(when));
</script>

<template>
  <span :title="formatZoned(whenTime)">{{ useTimeAgo(when.epochMilliseconds) }}</span>
</template>
