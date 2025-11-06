<script setup lang="ts">
import type { Direction } from "@/api/types.ts";
import { formatValue } from "@/lib/format.ts";
import { cn, getGrade } from "@/lib/utils.ts";
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
  direction: Direction;
}>();

const amount = computed(() => {
  if (from === undefined || to === undefined) return undefined;
  return to - from;
});

const grade = computed(() => {
  if (amount.value === undefined || amount.value === 0) return undefined;
  return getGrade(amount.value, direction);
});
</script>

<template>
  <template v-if="amount === undefined" />
  <div
    v-else
    :class="
      cn('text-right whitespace-pre', {
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
