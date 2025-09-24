<script setup lang="ts">
import { type ColumnDef } from "@tanstack/vue-table";
import { h } from "vue";
import CTableCellValue from "@/components/CTableCellValue.vue";
import CTableCellDelta from "@/components/CTableCellDelta.vue";
import CTable from "@/components/CTable.vue";

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
</script>

<template>
  <CTable :columns="columns" :data="measurements" />
</template>
