<script setup lang="ts" generic="T">
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlFilter from "@/components/CControlFilter.vue";
import CControlRow from "@/components/CControlRow.vue";
import {
  type ColumnDef,
  type ColumnSort,
  FlexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  type Row,
  useVueTable,
} from "@tanstack/vue-table";
import { computed, ref, watch } from "vue";

const {
  columns,
  data,
  onClickRow = undefined,
  initialSort = undefined,
} = defineProps<{
  columns: ColumnDef<T>[];
  data: T[];
  onClickRow?: (row: Row<T>) => void;
  initialSort?: ColumnSort;
}>();

const filter = defineModel<string>("filter", { required: true });

const nColums = computed(() => columns.length);

const tableData = useVueTable({
  columns,
  get data() {
    return data;
  },
  state: {
    get globalFilter() {
      return filter.value;
    },
  },
  initialState: {
    sorting: initialSort ? [initialSort] : [],
  },
  getCoreRowModel: getCoreRowModel(),
  getSortedRowModel: getSortedRowModel(),
  getPaginationRowModel: getPaginationRowModel(),
});

const pageSizes = [5, 100, 500, 1000, 5000];
const pageSize = ref<number>(100);
watch(pageSize, (newValue) => {
  tableData.setPageSize(newValue);
});
tableData.setPageSize(pageSize.value);

function goToFirstPage() {
  tableData.setPageIndex(0);
}

function goToPreviousPage() {
  const index = tableData.getState().pagination.pageIndex;
  const nextIndex = Math.max(index - 1, 0);
  tableData.setPageIndex(nextIndex);
}

function goToNextPage() {
  const index = tableData.getState().pagination.pageIndex;
  const nextIndex = Math.min(index + 1, tableData.getPageCount() - 1);
  tableData.setPageIndex(nextIndex);
}

function goToLastPage() {
  const index = Math.max(0, tableData.getPageCount() - 1);
  tableData.setPageIndex(index);
}
</script>

<template>
  <div class="flex w-fit flex-col gap-1">
    <CControlFilter v-model="filter" placeholder="Enter a regex..." />

    <CControl>
      <CControlRow>
        <CButton @click="goToFirstPage()">First</CButton>
        <CButton @click="goToPreviousPage()">Prev</CButton>
        <div>Page {{ tableData.getState().pagination.pageIndex + 1 }} / {{ tableData.getPageCount() }}</div>
        <CButton @click="goToNextPage()">Next</CButton>
        <CButton @click="goToLastPage()">Last</CButton>
        <label class="ml-auto">
          Rows:
          <select v-model="pageSize" class="bg-background h-5 px-1">
            <option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option>
          </select>
        </label>
      </CControlRow>
    </CControl>

    <div>Use shift+click to sort by multiple columns simultaneously.</div>

    <table class="mt-1 w-fit">
      <thead>
        <tr v-for="headerGroup in tableData.getHeaderGroups()" :key="headerGroup.id">
          <th
            v-for="header in headerGroup.headers"
            :key="header.id"
            :colspan="header.colSpan"
            class="pl-4 first:pl-0"
            @click="header.column.getToggleSortingHandler()?.($event)"
          >
            <FlexRender :render="header.column.columnDef.header" :props="header.getContext()" />
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in tableData.getRowModel().rows"
          :key="row.id"
          :class="[
            'border-background-alt hover:bg-background-alt border-t-[1px] border-dashed',
            { 'cursor-pointer': onClickRow },
          ]"
          @click.stop.prevent="onClickRow && onClickRow(row)"
        >
          <td v-for="cell in row.getVisibleCells()" :key="cell.id" class="py-[1px] pl-4 first:pl-0">
            <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
table {
  grid-template-columns: repeat(v-bind(nColums), auto);
}
</style>
