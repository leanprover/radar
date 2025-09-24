<script setup lang="ts" generic="T">
import {
  type ColumnDef,
  FlexRender,
  getCoreRowModel,
  getSortedRowModel,
  type SortingState,
  useVueTable,
} from "@tanstack/vue-table";
import { computed, ref } from "vue";

const { columns, data } = defineProps<{ columns: ColumnDef<T>[]; data: T[] }>();

const nColums = computed(() => columns.length);

const sorting = ref<SortingState>([]);

const tableData = useVueTable({
  columns,
  data,
  state: {
    get sorting() {
      return sorting.value;
    },
  },
  onSortingChange(updaterOrValue) {
    sorting.value = typeof updaterOrValue === "function" ? updaterOrValue(sorting.value) : updaterOrValue;
  },
  getCoreRowModel: getCoreRowModel(),
  getSortedRowModel: getSortedRowModel(),
});
</script>

<template>
  <table class="grid w-fit gap-x-4">
    <thead class="contents">
      <tr v-for="headerGroup in tableData.getHeaderGroups()" :key="headerGroup.id" class="contents">
        <th
          v-for="header in headerGroup.headers"
          :key="header.id"
          :colspan="header.colSpan"
          @click="header.column.getToggleSortingHandler()?.($event)"
        >
          <FlexRender :render="header.column.columnDef.header" :props="header.getContext()" />
        </th>
      </tr>
    </thead>
    <tbody class="contents">
      <tr v-for="row in tableData.getRowModel().rows" :key="row.id" class="contents">
        <td v-for="cell in row.getVisibleCells()" :key="cell.id">
          <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
table {
  grid-template-columns: repeat(v-bind(nColums), auto);
}
</style>
