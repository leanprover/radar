<script setup lang="ts">
import type { Direction } from "@/api/types.ts";
import { formatValue } from "@/lib/format.ts";
import { getGrade } from "@/lib/utils.ts";
import { computed } from "vue";

const {
  amount,
  unit = undefined,
  direction = 0,
} = defineProps<{
  amount: number;
  unit?: string;
  direction?: Direction;
}>();

const colors = { good: "text-green", bad: "text-red", neutral: undefined };
const color = computed(() => colors[getGrade(amount, direction)]);
</script>

<template>
  <span :class="['font-bold', color]" :title="amount.toString()"
    >{{ formatValue(amount, unit, { sign: true })
    }}<template v-if="unit !== undefined && unit !== 's'"> {{ unit }}</template></span
  >
</template>
