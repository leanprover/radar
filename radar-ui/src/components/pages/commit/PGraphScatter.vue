<script setup lang="ts">
import { JsonMetricComparison } from "@/api/types.ts";
import CBrutzelBox from "@/components/CBrutzelBox.vue";
import CControl from "@/components/CControl.vue";
import CControlRow from "@/components/CControlRow.vue";
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
import Annotation from "chartjs-plugin-annotation";
import { computed, ref } from "vue";
import { Scatter } from "vue-chartjs";

const emit = defineEmits<{ filter: [metrics: string[]] }>();
const { measurements } = defineProps<{ measurements: JsonMetricComparison[] }>();

const log = ref(true);

// Modules we don't register here may get minimized out later.
// https://www.chartjs.org/docs/latest/getting-started/integration.html#bundle-optimization
ChartJS.register(Annotation, Legend, LinearScale, LogarithmicScale, PointElement, ScatterController, Tooltip);

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

const PADDING = 0.02; // of the width from min to max

function padLinear(min: number, max: number): { min: number; max: number } {
  const delta = (max - min) * PADDING;
  return { min: min - delta, max: max + delta };
}

function padLog(min: number, max: number): { min: number; max: number } {
  // y = log(x)
  // y_min = log(min)
  // y_min' = log(min) - [ log(max) - log(min) ] * PADDING
  //        = log(min) - [ log(max) * PADDING - log(min) * PADDING ]
  //        = log(min) - [ log(max^PADDING) - log(min^PADDING) ]
  //        = log(min) - log(max^PADDING / min^PADDING)
  //        = log(min / [max^PADDING / min^PADDING])
  //        = log(min / (max / min)^PADDING)
  const delta = Math.pow(max / min, PADDING);
  return { min: min / delta, max: max * delta };
}

function pad(min: number, max: number, log: boolean): { min: number; max: number } {
  // return { min, max };
  if (log) return padLog(min, max);
  return padLinear(min, max);
}

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

const options = computed<ChartOptions<"scatter">>(() => {
  const type = log.value ? "logarithmic" : "linear";
  const padded = pad(min.value, max.value, log.value);
  return {
    responsive: true,
    aspectRatio: 1,
    animation: false,
    scales: {
      x: {
        title: { display: true, text: "Reference commit" },
        type,
        position: "bottom",
        ticks: { callback: formatTick },
        min: padded.min,
        max: padded.max,
      },
      y: {
        title: { display: true, text: "Commit" },
        type,
        position: "left",
        ticks: { callback: formatTick },
        min: padded.min,
        max: padded.max,
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
      annotation: {
        annotations: {
          line: {
            type: "line",
            borderWidth: 1,
            borderColor: "#ddd",
            drawTime: "beforeDatasetsDraw",
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
  };
});
</script>

<template>
  <div class="flex flex-col gap-2">
    <CControlRow>
      <CControl>
        <label>Logarithmic: <input v-model="log" type="checkbox" /></label>
      </CControl>
    </CControlRow>
    <CBrutzelBox>
      <Scatter id="scatter" :options :data />
    </CBrutzelBox>
  </div>
</template>
