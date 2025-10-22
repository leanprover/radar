<script setup lang="ts">
import { formatValue } from "@/lib/format.ts";
import { computed } from "vue";

const {
  amount,
  unit = undefined,
  direction = 0,
} = defineProps<{
  amount: number;
  unit?: string;
  direction?: -1 | 0 | 1;
}>();

const color = computed(() => {
  const sign = Math.sign(amount);
  if (sign === direction) return "text-green";
  if (sign === -direction) return "text-red";
  return undefined;
});
</script>

<template>
  <span :class="['font-bold', color]"
    >{{ formatValue(amount, unit, { sign: true })
    }}<template v-if="unit !== undefined && unit !== 's'"> {{ unit }}</template></span
  >
</template>
