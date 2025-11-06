<script setup lang="ts">
import { JsonMetricComparison } from "@/api/types.ts";
import CTable from "@/components/table/CTable.vue";
import CTableCellDelta from "@/components/table/CTableCellDelta.vue";
import CTableCellValue from "@/components/table/CTableCellValue.vue";
import CTableHeader from "@/components/table/CTableHeader.vue";
import { metricFilterMatches } from "@/lib/utils.ts";
import { type ColumnDef, type Row } from "@tanstack/vue-table";
import { h } from "vue";
import { useRouter } from "vue-router";

const { repo, measurements } = defineProps<{ repo: string; measurements: JsonMetricComparison[] }>();
const filter = defineModel<string>("filter", { required: true });
const router = useRouter();

const columns: ColumnDef<JsonMetricComparison>[] = [
  {
    id: "metric",
    accessorFn: (it) => it.metric.split("//")[0] ?? it.metric,
    sortingFn: "textCaseSensitive",
    sortDescFirst: true,
    header: ({ table, column }) =>
      h(CTableHeader<JsonMetricComparison>, {
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
      h(CTableHeader<JsonMetricComparison>, {
        table,
        column,
        title: "Submetric",
        align: "left",
      }),
  },
  {
    id: "value",
    accessorFn: (it) => it.second,
    header: ({ table, column }) =>
      h(CTableHeader<JsonMetricComparison>, {
        table,
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
    header: ({ table, column }) =>
      h(CTableHeader<JsonMetricComparison>, {
        table,
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
    id: "deltapercent",
    accessorFn: (it) => {
      if (it.first === undefined) return undefined;
      if (it.second === undefined) return undefined;
      if (it.first === 0) return undefined;
      return (it.second - it.first) / it.first;
    },
    header: ({ table, column }) =>
      h(CTableHeader<JsonMetricComparison>, {
        table,
        column,
        title: "Delta%",
        align: "right",
      }),
    cell: ({ row, cell }) =>
      h(CTableCellDelta, {
        from: 0,
        to: cell.getValue() as number | undefined,
        unit: "100%",
        direction: row.original.direction,
      }),
  },
  {
    accessorKey: "unit",
    header: ({ table, column }) =>
      h(CTableHeader<JsonMetricComparison>, {
        table,
        column,
        title: "Unit",
        align: "left",
      }),
  },
  {
    accessorKey: "secondSource",
    header: ({ table, column }) =>
      h(CTableHeader<JsonMetricComparison>, {
        table,
        column,
        title: "Source",
        align: "right",
      }),
  },
];

function filterFn(row: Row<JsonMetricComparison>, col: string, data: string): boolean {
  if (col !== "metric") return false;
  return metricFilterMatches(data, row.original.metric);
}

function onClickRow(row: Row<JsonMetricComparison>): void {
  void router.push({
    name: "/repos.[repo].graph",
    params: { repo },
    query: { m: row.original.metric, s: filter.value === "" ? undefined : filter.value },
  });
}
</script>

<template>
  <CTable
    v-model:filter="filter"
    :columns="columns"
    :data="measurements"
    :filter-fn="filterFn"
    :on-click-row="onClickRow"
  />
</template>
