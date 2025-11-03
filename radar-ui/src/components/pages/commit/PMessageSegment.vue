<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import CDeltaAmount from "@/components/format/CDeltaAmount.vue";
import CDeltaPercent from "@/components/format/CDeltaPercent.vue";

const { segment } = defineProps<{ segment: JsonMessageSegment }>();
</script>

<template>
  <CDeltaAmount
    v-if="segment.type === 'delta'"
    :amount="segment.amount"
    :unit="segment.unit"
    :direction="segment.direction"
  />
  <CDeltaPercent v-else-if="segment.type === 'deltaPercent'" :factor="segment.factor" :direction="segment.direction" />
  <span
    v-else-if="segment.type === 'exitCode'"
    :class="{ 'font-bold': true, 'text-green': segment.exitCode === 0, 'text-red': segment.exitCode !== 0 }"
    >{{ segment.exitCode }}</span
  >
  <span v-else-if="segment.type === 'metric'" class="italic">{{ segment.metric }}</span>
  <span v-else-if="segment.type === 'run'" class="italic">{{ segment.run }}</span>
  <span v-else>{{ segment.text }}</span>
</template>
