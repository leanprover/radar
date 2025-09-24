<script setup lang="ts">
import { formatValue } from "@/lib/utils.ts";
import { computed } from "vue";

const {
  from = undefined,
  to = undefined,
  unit = undefined,
  direction,
} = defineProps<{
  from?: number;
  to?: number;
  unit?: string;
  direction: -1 | 0 | 1;
}>();

const amount = computed(() => {
  if (from === undefined || to === undefined) return undefined;
  return to - from;
});

const grade = computed(() => {
  if (amount.value === undefined) return undefined;
  const sign = Math.sign(amount.value);
  if (sign === 0) return undefined;
  if (direction === 0) return "neutral";
  if (sign === direction) return "good";
  if (sign === -direction) return "bad";
  return undefined; // This shouldn't happen unless NaN is involved
});

const opts = { align: true, sign: true };
</script>

<template>
  <template v-if="amount === undefined" />
  <div v-else-if="grade === 'good'" class="text-green text-right font-bold">{{ formatValue(amount, unit, opts) }}</div>
  <div v-else-if="grade === 'bad'" class="text-red text-right font-bold">{{ formatValue(amount, unit, opts) }}</div>
  <div v-else-if="grade === 'neutral'" class="text-right font-bold">
    {{ formatValue(amount, unit, opts) }}
  </div>
  <div v-else class="text-foreground-alt text-right">{{ formatValue(amount, unit, opts) }}</div>
</template>
