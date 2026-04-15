<script setup lang="ts">
import CTimeAgo from "@/components/format/CTimeAgo.vue";
import CLink from "@/components/link/CLink.vue";
import { Temporal } from "temporal-polyfill";

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
    <CLink
      ><RouterLink :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash } }">{{
        title
      }}</RouterLink></CLink
    >
    {{ " " }}
    <span class="text-foreground-alt text-xs"><CTimeAgo :when="time" /> by {{ author }}</span>
  </span>
</template>
