<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import CDeltaAmount from "@/components/format/CDeltaAmount.vue";
import CDeltaPercent from "@/components/format/CDeltaPercent.vue";
import CLink from "@/components/link/CLink.vue";
import { escapeMetrics } from "@/lib/utils.ts";

const { repo, chash, reference, segment } = defineProps<{
  repo: string;
  chash: string;
  reference: string;
  segment: JsonMessageSegment;
}>();
</script>

<template>
  <CDeltaAmount
    v-if="segment.type === 'delta'"
    :amount="segment.amount"
    :unit="segment.unit"
    :goodness="segment.goodness"
  />
  <CDeltaPercent v-else-if="segment.type === 'deltaPercent'" :factor="segment.factor" :goodness="segment.goodness" />
  <span
    v-else-if="segment.type === 'exitCode'"
    :class="{ 'font-bold': true, 'text-green': segment.exitCode === 0, 'text-red': segment.exitCode !== 0 }"
    >{{ segment.exitCode }}</span
  >
  <CLink v-else-if="segment.type === 'metric'" class="italic">
    <RouterLink
      :to="{
        name: '/repos.[repo].commits.[chash]',
        params: { repo, chash },
        query: { reference: reference || undefined, s: escapeMetrics([segment.metric]) },
      }"
      >{{ segment.metric }}</RouterLink
    >
  </CLink>
  <CLink v-else-if="segment.type === 'run'" class="italic">
    <RouterLink :to="{ name: '/repos.[repo].commits.[chash].runs.[run]', params: { repo, chash, run: segment.run } }">{{
      segment.run
    }}</RouterLink>
  </CLink>
  <span v-else>{{ segment.text }}</span>
</template>
