<script setup lang="ts">
import type { JsonMessage } from "@/api/types.ts";
import PDetailsMessageSegment from "@/components/pages/commit/PDetailsMessageSegment.vue";

const { repo, chash, message } = defineProps<{ repo: string; chash: string; message: JsonMessage }>();
</script>

<template>
  <div>
    <PDetailsMessageSegment v-for="(segment, i) in message.segments" :key="i" :repo :chash :segment="segment" />
  </div>
</template>
