<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import PCommitInfo from "@/components/pages/graph/PCommitInfo.vue";
import PMetricSelector from "@/components/pages/graph/PMetricSelector.vue";
import { useRepo } from "@/composables/useRepo.ts";
import { useRepoGraph } from "@/composables/useRepoGraph.ts";
import { formatValue } from "@/lib/format.ts";
import { useQueryParamAsBool, useQueryParamAsInt, useQueryParamAsStringSet } from "@/lib/query.ts";
import type uPlot from "uplot";
import UplotVue from "uplot-vue";
import { computed, reactive, ref } from "vue";
import { useRoute } from "vue-router";

const metricLimit = 100;

const route = useRoute("/repos.[repo].graph");
const repo = useRepo(route.params.repo);

const queryM = useQueryParamAsStringSet("m");
const queryN = useQueryParamAsInt("n", 100, { min: 100, max: 1000 });
const queryZero = useQueryParamAsBool("zero", true);
const queryNormalize = useQueryParamAsBool("normalize", false);

const graph = reactive(
  useRepoGraph(
    () => route.params.repo,
    () => Array.from(queryM.value).sort(),
    queryN,
  ),
);

const hoverIdx = ref<number>();
const hoverCommit = computed(() => {
  if (hoverIdx.value === undefined) return;
  return graph.data?.commits[hoverIdx.value];
});

const options = computed<uPlot.Options>(() => {
  const series = [];
  if (graph.data)
    for (const metric of graph.data.metrics)
      series.push({
        label: metric.metric,
        stroke: "red",
      });

  return {
    width: 670,
    height: 400,
    axes: [
      {},
      {
        size: 70,
        values: (_, vals) => vals.map((it) => formatValue(it)),
      },
    ],
    scales: {
      x: { time: false },
      y: { range: [queryZero.value ? 0 : null, null] },
    },
    series: [
      {
        label: "sha",
        value: (_self, rawValue) => graph.data?.commits[rawValue]?.chash.slice(0, 12) ?? "--",
      },
      ...series,
    ],
    cursor: { lock: true },
    hooks: {
      setCursor: [(plot) => (hoverIdx.value = plot.cursor.idx ?? undefined)],
    },
  };
});

function normalize(values: (number | null)[]): (number | null)[] {
  const lastValue = values.find((it) => it !== null);
  if (lastValue === undefined || lastValue === 0) return values;
  return values.map((it) => it && it / lastValue);
}

const data = computed<uPlot.AlignedData>(() => {
  if (!graph.data) return [[]];
  const indices = graph.data.commits.map((_, i) => i);
  const measurements = graph.data.metrics
    .map((it) => it.measurements)
    .map((it) => (queryNormalize.value ? normalize(it) : it));
  return [indices, ...measurements];
});
</script>

<template>
  <CSection>
    <CSectionTitle>Graph</CSectionTitle>

    <div class="flex gap-8">
      <div class="flex w-[80ch] flex-col gap-4">
        <div class="flex flex-wrap gap-1">
          <label class="bg-background-alt w-fit p-1 align-baseline select-none">
            n:
            <input v-model="queryN" type="number" min="100" max="1000" step="100" class="bg-background px-1" />
          </label>
          <label class="bg-background-alt w-fit p-1 align-baseline select-none">
            Start at 0:
            <input v-model="queryZero" type="checkbox" />
          </label>
          <label class="bg-background-alt w-fit p-1 align-baseline select-none">
            Normalize:
            <input v-model="queryNormalize" type="checkbox" />
          </label>
        </div>

        <div class="flex flex-col items-center bg-white text-black">
          <UplotVue key="graph" :options :data />
          <div class="hidden text-[0.6em] dark:block">Sorry for brutzeling your eye balls.</div>
        </div>

        <PCommitInfo
          v-if="hoverCommit"
          :repo="route.params.repo"
          :url="repo?.url"
          :chash="hoverCommit.chash"
          :author="hoverCommit.author"
          :title="hoverCommit.title"
          :body="hoverCommit.body"
        />
      </div>

      <PMetricSelector v-model:selected="queryM" :repo="route.params.repo" :limit="metricLimit" class="grow" />
    </div>
  </CSection>
</template>
