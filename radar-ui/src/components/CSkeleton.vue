<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils.ts";

const { class: clazz = undefined, error } = defineProps<{
  error: Error | null | undefined;
  class?: HTMLAttributes["class"];
}>();
</script>

<template>
  <Skeleton :class="cn(clazz, { 'bg-destructive animate-none': error })" :title="error?.message" />
</template>
