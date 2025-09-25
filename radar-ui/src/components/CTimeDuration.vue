<script setup lang="ts">
import { Temporal } from "temporal-polyfill";
import { formatDuration } from "@/lib/utils.ts";

const { duration } = defineProps<{ duration: Temporal.Duration }>();
</script>

<template>
  <span>{{ formatDuration(duration) }}</span>
</template>
