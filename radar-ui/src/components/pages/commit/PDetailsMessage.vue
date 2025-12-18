<script setup lang="ts">
import type { JsonSignificance } from "@/api/types.ts";
import PDetailsMessageSegment from "@/components/pages/commit/PDetailsMessageSegment.vue";

const { repo, chash, reference, significance } = defineProps<{
  repo: string;
  chash: string;
  reference: string;
  significance: JsonSignificance;
}>();
</script>

<template>
  <div>
    <PDetailsMessageSegment
      v-for="(segment, i) in significance.segments"
      :key="i"
      :repo
      :chash
      :reference
      :segment="segment"
    />
  </div>
</template>
