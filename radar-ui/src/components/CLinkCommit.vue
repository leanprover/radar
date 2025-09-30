<script setup lang="ts">
import { Temporal } from "temporal-polyfill";
import CTimeAgo from "@/components/CTimeAgo.vue";

const { repo, chash, title, author, time } = defineProps<{
  repo: string;
  chash: string;
  title: string;
  author: string;
  time: Temporal.Instant;
}>();
</script>

<template>
  <span>
    <RouterLink :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash } }" class="hover:underline">
      {{ title }}
    </RouterLink>
    {{ " " }}
    <span class="text-foreground-alt text-xs"><CTimeAgo :when="time" /> by {{ author }}</span>
  </span>
</template>
