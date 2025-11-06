<script setup lang="ts">
import { JsonMetricComparison } from "@/api/types.ts";
import { formatValue } from "@/lib/format.ts";
import { getGrade, type Grade } from "@/lib/utils.ts";
import {
  Chart as ChartJS,
  type ChartData,
  type ChartOptions,
  Legend,
  LinearScale,
  LogarithmicScale,
  PointElement,
  ScatterController,
  Tooltip,
} from "chart.js";
import { computed } from "vue";
import { Scatter } from "vue-chartjs";

const emit = defineEmits<{ filter: [metrics: string[]] }>();
const { measurements } = defineProps<{ measurements: JsonMetricComparison[] }>();

// Modules we don't register here may get minimized out later.
// https://www.chartjs.org/docs/latest/getting-started/integration.html#bundle-optimization
ChartJS.register(Legend, LinearScale, LogarithmicScale, PointElement, ScatterController, Tooltip);

interface Evaluated {
  metric: string;
  unit?: string;
  first: number;
  second: number;
  x: number;
  y: number;
  grade: Grade;
}

const evaluated = computed<Evaluated[]>(() =>
  measurements
    .map((it) => {
      if (it.first === undefined) return undefined;
      if (it.second === undefined) return undefined;
      return {
        metric: it.metric,
        unit: it.unit,
        first: it.first,
        second: it.second,
        x: it.first,
        y: it.second,
        grade: getGrade([it.first, it.second], it.direction),
      };
    })
    .filter((it) => it !== undefined),
);

//////////
// Data //
//////////

const good = computed(() => evaluated.value.filter((it) => it.grade === "good"));
const bad = computed(() => evaluated.value.filter((it) => it.grade === "bad"));
const neutral = computed(() => evaluated.value.filter((it) => it.grade === "neutral"));

const data = computed<ChartData<"scatter">>(() => {
  return {
    datasets: [
      {
        label: "good",
        data: good.value,
        backgroundColor: "#0a0",
      },
      {
        label: "bad",
        data: bad.value,
        backgroundColor: "#f00",
        pointStyle: "rect",
        pointRadius: 3.5,
      },
      {
        label: "neutral",
        data: neutral.value,
        backgroundColor: "#aaa",
        pointStyle: "triangle",
        pointRadius: 3.5,
      },
    ],
  };
});

function getEvaluated(datasetIndex: number, index: number): Evaluated | undefined {
  const dataset = [good.value, bad.value, neutral.value][datasetIndex];
  if (dataset === undefined) return undefined;
  return dataset[index];
}

/////////////
// Options //
/////////////

const LOG_PADDING = 1.5;

const min = computed(() => {
  const result = Math.min(...evaluated.value.map((it) => Math.min(it.x, it.y)));
  if (result === Infinity) return 0;
  return result;
});

const max = computed(() => {
  const result = Math.max(...evaluated.value.map((it) => Math.max(it.x, it.y)));
  if (result === -Infinity) return 1;
  return result;
});

function formatTick(value: string | number): string {
  if (typeof value === "string") return value;
  return formatValue(value);
}

const options = computed<ChartOptions<"scatter">>(() => ({
  responsive: true,
  aspectRatio: 1,
  animation: false,
  scales: {
    x: {
      title: { display: true, text: "Reference commit" },
      type: "logarithmic",
      position: "bottom",
      ticks: { callback: formatTick },
      min: min.value / LOG_PADDING,
      max: max.value * LOG_PADDING,
    },
    y: {
      title: { display: true, text: "Commit" },
      type: "logarithmic",
      position: "left",
      ticks: { callback: formatTick },
      min: min.value / LOG_PADDING,
      max: max.value * LOG_PADDING,
    },
  },
  plugins: {
    legend: {
      position: "bottom",
    },
    tooltip: {
      callbacks: {
        label(item) {
          const value = item.raw as Evaluated;
          const firstStr = formatValue(value.first, value.unit);
          const secondStr = formatValue(value.second, value.unit);
          const deltaStr = formatValue(value.second - value.first, value.unit, { sign: true });
          return `${value.metric}: (${firstStr} → ${secondStr}) ${deltaStr}`;
        },
      },
    },
  },
  onClick(_, elements) {
    let metrics = elements
      .map((it) => getEvaluated(it.datasetIndex, it.index))
      .filter((it) => it !== undefined)
      .map((it) => it.metric);
    emit("filter", metrics);
  },
}));
</script>

<template>
  <div class="overflow-clip">
    <div class="w-[600px]">
      <Scatter id="scatter" :options :data />
    </div>
  </div>
</template>
