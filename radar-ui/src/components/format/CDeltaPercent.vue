<script setup lang="ts">
import type { JsonMessageGoodness } from "@/api/types.ts";
import { formatValue } from "@/lib/format.ts";
import { computed } from "vue";

const { factor, goodness } = defineProps<{ factor: number; goodness: JsonMessageGoodness }>();

const colors = { GOOD: "text-green", BAD: "text-red", NEUTRAL: undefined };
const color = computed(() => colors[goodness]);
</script>

<template>
  <span :class="['font-bold', color]" :title="factor.toString()">{{
    formatValue(factor, "100%", { sign: true })
  }}</span>
</template>
