<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import CDeltaAmount from "@/components/CDeltaAmount.vue";
import CDeltaPercent from "@/components/CDeltaPercent.vue";

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
  <span v-else-if="segment.type === 'metric'" class="italic">{{ segment.metric }}</span>
  <span v-else>{{ segment.text }}</span>
</template>
