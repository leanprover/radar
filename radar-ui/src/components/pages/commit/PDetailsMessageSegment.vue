<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import CDeltaAmount from "@/components/format/CDeltaAmount.vue";
import CDeltaPercent from "@/components/format/CDeltaPercent.vue";
import { escapeRegex } from "@/lib/utils.ts";

const { repo, chash, segment } = defineProps<{ repo: string; chash: string; segment: JsonMessageSegment }>();
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
  <RouterLink
    v-else-if="segment.type === 'metric'"
    :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash }, query: { s: escapeRegex(segment.metric) } }"
    class="italic hover:underline"
  >
    {{ segment.metric }}
  </RouterLink>
  <RouterLink
    v-else-if="segment.type === 'run'"
    :to="{ name: '/repos.[repo].commits.[chash].runs.[run]', params: { repo, chash, run: segment.run } }"
    class="italic hover:underline"
  >
    {{ segment.run }}
  </RouterLink>
  <span v-else>{{ segment.text }}</span>
</template>
