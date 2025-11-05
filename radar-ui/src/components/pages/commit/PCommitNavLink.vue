<script setup lang="ts">
import type { JsonLinkedCommit } from "@/api/commit.ts";
import { cn } from "@/lib/utils.ts";

const { prefix, repo, search, commit } = defineProps<{
  prefix: string;
  repo: string;
  search: string | undefined;
  commit: JsonLinkedCommit;
}>();
</script>

<template>
  <RouterLink
    :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash: commit.chash }, query: { s: search } }"
    :title="commit.title"
    :class="cn('cursor-pointer truncate italic hover:underline', { 'text-foreground-alt': !commit.tracked })"
  >
    {{ prefix }} {{ commit.title }}
  </RouterLink>
</template>
