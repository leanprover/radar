<script setup lang="ts">
import { cn } from "@/lib/utils.ts";

const {
  repo,
  chash,
  title,
  tracked,
  search = undefined,
} = defineProps<{
  repo: string;
  chash: string;
  title: string;
  tracked: boolean;
  search?: string;
}>();
</script>

<template>
  <RouterLink
    :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash }, query: { s: search || undefined } }"
    :title="title"
    :class="cn('cursor-pointer truncate italic hover:underline', { 'text-foreground-alt': !tracked })"
  >
    &lt; {{ title }}
  </RouterLink>
</template>
