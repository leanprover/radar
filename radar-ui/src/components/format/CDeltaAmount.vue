<script setup lang="ts">
import type { JsonMessageGoodness } from "@/api/types.ts";
import { formatValue } from "@/lib/format.ts";
import { computed } from "vue";

const {
  amount,
  unit = undefined,
  goodness,
} = defineProps<{
  amount: number;
  unit?: string;
  goodness: JsonMessageGoodness;
}>();

const colors = { GOOD: "text-green", BAD: "text-red", NEUTRAL: undefined };
const color = computed(() => colors[goodness]);
</script>

<template>
  <span :class="['font-bold', color]" :title="amount.toString()"
    >{{ formatValue(amount, unit, { sign: true })
    }}<template v-if="unit !== undefined && unit !== 's'"> {{ unit }}</template></span
  >
</template>
