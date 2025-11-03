<script setup lang="ts">
import { formatDuration } from "@/lib/format.ts";
import { Temporal } from "temporal-polyfill";

const { duration } = defineProps<{ duration: Temporal.Duration }>();
</script>

<template>
  <span>{{ formatDuration(duration) }}</span>
</template>
