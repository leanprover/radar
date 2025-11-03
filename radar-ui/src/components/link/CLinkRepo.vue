<script setup lang="ts">
import { cn } from "@/lib/utils.ts";
import type { ClassValue } from "clsx";

const { repo, class: clazz = undefined } = defineProps<{ repo: string; class?: ClassValue }>();
</script>

<template>
  <RouterLink :to="{ name: '/repos.[repo]', params: { repo } }" :class="cn('hover:underline', clazz)">
    {{ repo }}
  </RouterLink>
</template>
