<script setup lang="ts" generic="T">
import { computed } from "vue";
import { type Column } from "@tanstack/vue-table";

const {
  column,
  title = undefined,
  align = "left",
} = defineProps<{
  column: Column<T>;
  title?: string;
  align?: "left" | "center" | "right";
}>();

const marker = computed(() => {
  const isSorted = column.getIsSorted();
  if (!isSorted) return " ";
  return { asc: "^", desc: "v" }[isSorted];
});

const index = computed(() => {
  const isSorted = column.getIsSorted();
  const canMultiSort = column.getCanMultiSort();
  if (!isSorted) return canMultiSort ? " " : "";
  return column.getSortIndex().toFixed();
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
