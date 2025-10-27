<script setup lang="ts">
import { formatValue } from "@/lib/format.ts";
import uPlot from "uplot";
import { computed, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from "vue";

const defaultWidth = 400;
const aspectRatio = 3 / 2;

const hoverIdx = defineModel<number | undefined>("hoverIdx", { required: true });
const {
  series,
  data,
  startAtZero = true,
} = defineProps<{
  series: uPlot.Series[];
  data: uPlot.AlignedData;
  startAtZero?: boolean;
}>();

const options = computed<uPlot.Options>(() => ({
  width: defaultWidth,
  height: defaultWidth / aspectRatio,

  axes: [
    {
      // Prevent fractional increments (there is no 0.1 commits)
      incrs: [1, 2, 5, 10, 20, 50, 100, 200, 500],
    },
    {
      size: 70,
      // TODO Format values with respect to y axis range
      values: (_, vals) => vals.map((it) => formatValue(it)),
    },
  ],

  scales: {
    x: { time: false },
    y: { range: [startAtZero ? 0 : null, null] },
  },

  // Maybe use tooltips instead of a live legend?
  // https://leeoniya.github.io/uPlot/demos/tooltips-closest.html
  // https://perf.rust-lang.org/index.html

  cursor: {
    lock: true,
    focus: { prox: 8 },
  },

  hooks: {
    setCursor: [
      (plot) => {
        // Make hoverIdx sticky: If it's ever defined, it should stay defined.
        // This prevents flickering in certain weird edge cases.
        if (plot.cursor.idx !== undefined && plot.cursor.idx !== null) {
          hoverIdx.value = plot.cursor.idx;
        }
      },
    ],
  },

  series,
}));

const base = useTemplateRef("base");
const plot = ref<uPlot>();
const observer = ref<ResizeObserver>(
  new ResizeObserver((entries) => {
    if (plot.value === undefined) return;

    const entry = entries[0];
    if (entry === undefined) return;

    const size = entry.contentBoxSize[0];
    if (size === undefined) return;

    const width = size.inlineSize;
    const height = width / aspectRatio;
    plot.value.setSize({ width, height });
  }),
);

function destroy() {
  if (plot.value) {
    plot.value.destroy();
    plot.value = undefined;
  }
  observer.value.disconnect();
}

function recreate() {
  destroy();
  if (!base.value) return;
  plot.value = new uPlot(options.value, data, base.value);
  observer.value.observe(base.value);
}

onMounted(() => {
  recreate();
});
onBeforeUnmount(() => {
  destroy();
});
watch(
  () => series,
  () => {
    if (!plot.value) return;
    // TODO Detect whether series actually changed
    recreate();
  },
);
watch(
  () => data,
  (newValue, oldValue) => {
    if (!plot.value) return;

    if (newValue.length !== oldValue.length) {
      recreate();
      return;
    }

    if (newValue[0].length !== oldValue[0].length) {
      recreate();
      return;
    }

    plot.value.setData(newValue, false);
    plot.value.redraw();
  },
);
watch(
  () => startAtZero,
  () => {
    if (!plot.value) return;
    recreate();
  },
);
</script>

<template>
  <div ref="base"></div>
</template>
