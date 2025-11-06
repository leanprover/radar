<script setup lang="ts">
import type { Direction } from "@/api/types.ts";
import { formatValue } from "@/lib/format.ts";
import { getGrade } from "@/lib/utils.ts";
import { computed } from "vue";

const { factor, direction = 0 } = defineProps<{
  factor: number;
  direction?: Direction;
}>();

const colors = { good: "text-green", bad: "text-red", neutral: undefined };
const color = computed(() => colors[getGrade(factor, direction)]);
</script>

<template>
  <span :class="['font-bold', color]" :title="factor.toString()">{{
    formatValue(factor, "100%", { sign: true })
  }}</span>
</template>
