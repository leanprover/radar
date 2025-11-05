<script setup lang="ts">
import type { Direction } from "@/api/types.ts";
import { formatValue } from "@/lib/format.ts";
import { computed } from "vue";

const { factor, direction = 0 } = defineProps<{
  factor: number;
  direction?: Direction;
}>();

const color = computed(() => {
  const sign = Math.sign(factor);
  if (sign === direction) return "text-green";
  if (sign === -direction) return "text-red";
  return undefined;
});
</script>

<template>
  <span :class="['font-bold', color]" :title="factor.toString()">{{
    formatValue(factor, "100%", { sign: true })
  }}</span>
</template>
