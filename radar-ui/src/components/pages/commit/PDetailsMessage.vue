<script setup lang="ts">
import type { JsonMessage } from "@/api/types.ts";
import PDetailsMessageSegment from "@/components/pages/commit/PDetailsMessageSegment.vue";

const { repo, chash, reference, message } = defineProps<{
  repo: string;
  chash: string;
  reference: string;
  message: JsonMessage;
}>();
</script>

<template>
  <div>
    <PDetailsMessageSegment
      v-for="(segment, i) in message.segments"
      :key="i"
      :repo
      :chash
      :reference
      :segment="segment"
    />
  </div>
</template>
