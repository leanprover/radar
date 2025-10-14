<script setup lang="ts">
import CTable from "@/components/CTable.vue";
import CTableHeader from "@/components/CTableHeader.vue";
import { type ColumnDef, type Row } from "@tanstack/vue-table";
import { computed, h } from "vue";

interface Metric {
  metric: string;
  unit?: string;
}

interface RichMetric extends Metric {
  selected: boolean;
}

const { metrics } = defineProps<{ metrics: Metric[] }>();
const filter = defineModel<string>("filter");
const selected = defineModel<Set<string>>("selected", { required: true });
const richMetrics = computed(() => metrics.map((it) => ({ selected: selected.value.has(it.metric), ...it })));

const columns: ColumnDef<RichMetric>[] = [
  {
    id: "selected",
    accessorKey: "selected",
    header: ({ table, column }) =>
      h(CTableHeader<RichMetric>, {
        table,
        column,
        title: "Visible",
        align: "center",
      }),
    cell: ({ row }) => h("div", { class: "text-center" }, row.original.selected ? "yes" : undefined),
  },
  {
    id: "metric",
    accessorFn: (it) => it.metric.split("//")[0] ?? it.metric,
    sortingFn: "textCaseSensitive",
    sortDescFirst: true,
    header: ({ table, column }) =>
      h(CTableHeader<RichMetric>, {
        table,
        column,
        title: "Metric",
        align: "left",
      }),
  },
  {
    id: "separator",
    accessorFn: () => "//",
    header: () => null,
  },
  {
    id: "submetric",
    accessorFn: (it) => it.metric.split("//")[1],
    sortingFn: "textCaseSensitive",
    sortDescFirst: true,
    header: ({ table, column }) =>
      h(CTableHeader<RichMetric>, {
        table,
        column,
        title: "Submetric",
        align: "left",
      }),
  },
  {
    accessorKey: "unit",
    header: ({ table, column }) =>
      h(CTableHeader<RichMetric>, {
        table,
        column,
        title: "Unit",
        align: "left",
      }),
  },
];

function filterFn(row: Row<RichMetric>, col: string, data: string): boolean {
  if (col !== "metric") return false;
  // Yes, this is incorrect according to Unicode, but it suffices for our metric names.
  return row.original.metric.toLowerCase().includes(data.toLowerCase());
}

function onClickRow(row: Row<RichMetric>): void {
  if (selected.value.has(row.original.metric)) {
    const result = new Set(selected.value);
    result.delete(row.original.metric);
    selected.value = result;
  } else {
    const result = new Set(selected.value);
    result.add(row.original.metric);
    selected.value = result;
  }
}
</script>

<template>
  <CTable
    v-model:filter="filter"
    :columns="columns"
    :data="richMetrics"
    :filter-fn="filterFn"
    :on-click-row="onClickRow"
    :initial-sort="{ id: 'selected', desc: true }"
  />
</template>
