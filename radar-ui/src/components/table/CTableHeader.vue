<script setup lang="ts" generic="T extends RowData">
import { type Column, type RowData, type Table } from "@tanstack/vue-table";
import { computed } from "vue";
import type { AppFeatures } from "./tableFeatures";

const {
  table,
  column,
  title = undefined,
  align = "left",
} = defineProps<{
  table: Table<AppFeatures, T>;
  column: Column<AppFeatures, T>;
  title?: string;
  align?: "left" | "center" | "right";
}>();

const marker = computed(() => {
  const isSorted = column.getIsSorted();
  if (!isSorted) return " ";
  return { asc: "^", desc: "v" }[isSorted];
});

const index = computed(() => {
  if (table.atoms.sorting.get().length <= 1) return ""; // Not multisorting
  if (!column.getIsSorted()) return " ";
  return (column.getSortIndex() + 1).toFixed();
});
</script>

<template>
  <div
    :class="{
      'cursor-pointer select-none': column.getCanSort(),
      'text-left': align === 'left',
      'text-center': align === 'center',
      'text-right': align === 'right',
    }"
  >
    <span v-if="column.getCanSort() && align === 'right'" class="text-xs font-normal whitespace-pre">
      {{ index }}{{ marker }}
    </span>

    <template v-if="title !== undefined">{{ title }}</template>
    <slot v-else />

    <span v-if="column.getCanSort() && align !== 'right'" class="text-xs font-normal whitespace-pre">
      {{ marker }}{{ index }}
    </span>
  </div>
</template>
