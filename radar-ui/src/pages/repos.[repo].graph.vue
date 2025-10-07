<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { reactive } from "vue";
import { useQueryParamAsBool, useQueryParamAsInt, useQueryParamAsStringArray } from "@/lib/utils.ts";
import UplotVue from "uplot-vue";
import { useRepoGraph } from "@/composables/useRepoGraph.ts";
import { useRoute } from "vue-router";
import CLoading from "@/components/CLoading.vue";
import { JsonGet } from "@/api/repoGraph.ts";
import type uPlot from "uplot";
import { formatValue } from "@/lib/format.ts";

const route = useRoute("/repos.[repo].graph");

const queryM = useQueryParamAsStringArray("m", { deduplicate: true, sort: true });
const queryN = useQueryParamAsInt("n", 100, { min: 100, max: 1000 });
const queryZero = useQueryParamAsBool("zero", true);

const graph = reactive(useRepoGraph(() => route.params.repo, queryM, queryN));

function mkOptions(data: JsonGet): uPlot.Options {
  const series = data.metrics.map((it) => ({
    label: it.metric,
    stroke: "red",
  }));

  return {
    width: 600,
    height: 400,
    axes: [
      {},
      {
        size: 70,
        values: (_, vals) => vals.map((it) => formatValue(it)),
      },
    ],
    scales: {
      x: {
        time: false,
      },
      y: {
        range: [queryZero.value ? 0 : null, null],
      },
    },
    series: [{}, ...series],
  };
}

function mkData(data: JsonGet): uPlot.AlignedData {
  const indices = data.chashes.map((_, i) => i);
  const measurements = data.metrics.map((it) => it.measurements);
  return [indices, ...measurements];
}
</script>

<template>
  <CSection>
    <CSectionTitle>Graph</CSectionTitle>

    <div class="-mx-1 flex flex-wrap gap-1">
      <label class="bg-background-alt w-fit p-1 align-baseline select-none">
        n:
        <input v-model="queryN" type="number" min="100" max="1000" step="100" class="bg-background px-1" />
      </label>
      <label class="bg-background-alt w-fit p-1 align-baseline select-none">
        Start at 0:
        <input v-model="queryZero" type="checkbox" />
      </label>
    </div>

    <CLoading v-if="!graph.isSuccess" :error="graph.error" />
    <UplotVue v-else :options="mkOptions(graph.data)" :data="mkData(graph.data)"></UplotVue>
  </CSection>
</template>
