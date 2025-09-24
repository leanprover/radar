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

const indicator = computed(() => {
  const isSorted = column.getIsSorted();
  const canMultiSort = column.getCanMultiSort();
  const sortIndex = column.getSortIndex().toFixed();

  if (!isSorted) return canMultiSort ? "  " : " ";
  const marker = { asc: "^", desc: "v" }[isSorted];
  return canMultiSort ? marker + sortIndex : marker;
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
    <span v-if="column.getCanSort() && align === 'right'" class="font-normal whitespace-pre">
      {{ indicator }}{{ " " }}
    </span>

    <template v-if="title !== undefined">{{ title }}</template>
    <slot v-else />

    <span v-if="column.getCanSort() && align !== 'right'" class="font-normal whitespace-pre">
      {{ " " }}{{ indicator }}
    </span>
  </div>
</template>
