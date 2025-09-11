<script setup lang="ts">
import type { HTMLAttributes } from "vue";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils.ts";

const { class: clazz, isError = false } = defineProps<{
  isError?: boolean;
  class?: HTMLAttributes["class"];
}>();
</script>

<template>
  <Skeleton :class="cn(clazz, { 'bg-destructive': isError })" />
</template>

<style scoped></style>
