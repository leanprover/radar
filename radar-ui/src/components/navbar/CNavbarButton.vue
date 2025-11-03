<script setup lang="ts">
import { cn } from "@/lib/utils.ts";

const { disabled = false, selected = false } = defineProps<{ disabled?: boolean; selected?: boolean }>();
</script>

<template>
  <button
    :class="
      cn('px-1', {
        'text-foreground-alt': disabled,
        'hover:bg-background-alt cursor-pointer': !disabled,
        'font-bold underline': selected,
      })
    "
    :disabled
  >
    <slot />
  </button>
</template>
