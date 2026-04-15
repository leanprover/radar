<script setup lang="ts">
import type { JsonLinkedCommit } from "@/api/commit.ts";
import CLink from "@/components/link/CLink.vue";

const { prefix, repo, search, commit } = defineProps<{
  prefix: string;
  repo: string;
  search: string | undefined;
  commit: JsonLinkedCommit;
}>();
</script>

<template>
  <CLink :class="{ 'truncate italic': true, 'text-foreground-alt': !commit.tracked }">
    <RouterLink
      :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash: commit.chash }, query: { s: search } }"
      :title="commit.title"
      >{{ prefix }} {{ commit.title }}</RouterLink
    >
  </CLink>
</template>
