<script setup lang="ts">
import { type ColumnDef } from "@tanstack/vue-table";
import { h } from "vue";
import CTableCellValue from "@/components/CTableCellValue.vue";
import CTableCellDelta from "@/components/CTableCellDelta.vue";
import CTable from "@/components/CTable.vue";
import CTableHeader from "@/components/CTableHeader.vue";

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
    sortingFn: "textCaseSensitive",
    sortDescFirst: true,
    header: ({ column }) =>
      h(CTableHeader<Measurement>, {
        column,
        title: "Metric",
        align: "left",
      }),
  },
  {
    id: "value",
    accessorFn: (it) => it.second,
    header: ({ column }) =>
      h(CTableHeader<Measurement>, {
        column,
        title: "Value",
        align: "right",
      }),
    cell: ({ row }) =>
      h(CTableCellValue, {
        value: row.original.second,
        unit: row.original.unit,
      }),
  },
  {
    id: "delta",
    accessorFn: (it) => {
      if (it.first === undefined) return 0;
      if (it.second === undefined) return 0;
      return it.second - it.first;
    },
    header: ({ column }) =>
      h(CTableHeader<Measurement>, {
        column,
        title: "Delta",
        align: "right",
      }),
    cell: ({ row }) =>
      h(CTableCellDelta, {
        from: row.original.first,
        to: row.original.second,
        unit: row.original.unit,
        direction: row.original.direction,
      }),
  },
  {
    accessorKey: "unit",
    header: ({ column }) =>
      h(CTableHeader<Measurement>, {
        column,
        title: "Unit",
        align: "left",
      }),
  },
];
</script>

<template>
  <CTable :columns="columns" :data="measurements" />
</template>
