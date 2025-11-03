<script setup lang="ts">
import { formatValue } from "@/lib/format.ts";

const { value = undefined, unit = undefined } = defineProps<{ value?: number; unit?: string }>();
</script>

<template>
  <template v-if="value === undefined" />
  <div v-else class="text-right whitespace-pre" :title="value.toString()">
    {{ formatValue(value, unit, { align: true }) }}
  </div>
</template>
