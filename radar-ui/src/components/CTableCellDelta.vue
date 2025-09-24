<script setup lang="ts">
import { cn, formatValue } from "@/lib/utils.ts";
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
</script>

<template>
  <template v-if="amount === undefined" />
  <div
    v-else
    :class="
      cn('text-right', {
        'text-green font-bold': grade === 'good',
        'text-red font-bold': grade === 'bad',
        'font-bold': grade === 'neutral',
        'text-foreground-alt': grade === undefined,
      })
    "
    :title="amount.toString()"
  >
    {{ formatValue(amount, unit, { align: true, sign: true }) }}
  </div>
</template>
