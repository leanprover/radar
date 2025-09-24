<script setup lang="ts">
import { type ColumnDef, FlexRender, getCoreRowModel, useVueTable } from "@tanstack/vue-table";
import { h } from "vue";
import CTableCellValue from "@/components/CTableCellValue.vue";
import CTableCellDelta from "@/components/CTableCellDelta.vue";

interface Measurement {
  metric: string;
  first?: number;
  second?: number;
  unit?: string;
  direction: -1 | 0 | 1;
}

const { measurements } = defineProps<{ measurements: Measurement[] }>();

const columns: ColumnDef<Measurement>[] = [
  {
    accessorKey: "metric",
    header: () => h("div", { class: "text-left" }, "Metric"),
  },
  {
    id: "value",
    accessorFn: (it) => it.second,
    header: () => h("div", { class: "text-right" }, "Value"),
    cell: ({ row }) =>
      h(CTableCellValue, {
        value: row.original.second,
        unit: row.original.unit,
      }),
  },
  {
    id: "delta",
    accessorFn: (it) => it.first,
    header: () => h("div", { class: "text-right" }, "Delta"),
    cell: ({ row }) =>
      h(CTableCellDelta, {
        from: row.original.first,
        to: row.original.second,
        unit: row.original.unit,
        direction: row.original.direction,
      }),
  },
];

const tableData = useVueTable({
  columns,
  data: measurements,
  getCoreRowModel: getCoreRowModel(),
});
</script>

<template>
  <table class="grid w-fit grid-cols-[auto_auto_auto] gap-x-4">
    <thead class="contents">
      <tr v-for="headerGroup in tableData.getHeaderGroups()" :key="headerGroup.id" class="contents">
        <th v-for="header in headerGroup.headers" :key="header.id" :colspan="header.colSpan">
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
