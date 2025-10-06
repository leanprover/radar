<script setup lang="ts">
import { cn } from "@/lib/utils.ts";

const { repo, chash, title, tracked } = defineProps<{ repo: string; chash: string; title: string; tracked: boolean }>();
</script>

<template>
  <RouterLink
    :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash } }"
    :title="title"
    :class="cn('cursor-pointer truncate italic hover:underline', { 'text-foreground-alt': !tracked })"
  >
    &gt; {{ title }}
  </RouterLink>
</template>
