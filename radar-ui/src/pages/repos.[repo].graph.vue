<script setup lang="ts">
import PCommitInfo from "@/components/pages/graph/PCommitInfo.vue";
import PMetricSelector from "@/components/pages/graph/PMetricSelector.vue";
import PUplot from "@/components/pages/graph/PUplot.vue";
import { useRepo } from "@/composables/useRepo.ts";
import { useRepoGraph } from "@/composables/useRepoGraph.ts";
import {
  useQueryParamAsBool,
  useQueryParamAsInt,
  useQueryParamAsString,
  useQueryParamAsStringSet,
} from "@/lib/query.ts";
import type uPlot from "uplot";
import { computed, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

const metricLimit = 100;

const nMin = 100;
const nMax = 1000;
const nStep = 100;
const nDefault = 200;

// https://sashamaps.net/docs/resources/20-colors/
const colors = [
  "#e6194B",
  "#3cb44b",
  "#ffe119",
  "#4363d8",
  "#f58231",
  "#911eb4",
  "#42d4f4",
  "#f032e6",
  "#bfef45",
  "#fabed4",
  "#469990",
  "#dcbeff",
  "#9A6324",
  "#fffac8",
  "#800000",
  "#aaffc3",
  "#808000",
  "#ffd8b1",
  "#000075",
  "#a9a9a9",
  "#000000",
];

const router = useRouter();
const route = useRoute("/repos.[repo].graph");
const repo = useRepo(route.params.repo);

const queryM = useQueryParamAsStringSet("m");
const queryN = useQueryParamAsInt("n", nDefault, { min: nMin, max: nMax });
const queryS = useQueryParamAsString("s");
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

const series = computed<uPlot.Series[]>(() => {
  const commitSeries: uPlot.Series = {
    label: "n",
  };

  const metricSeries: uPlot.Series[] = Array.from(queryM.value)
    .sort()
    .map((it, i) => ({
      label: it,
      stroke: colors[i % colors.length] ?? "red",
    }));

  return [commitSeries, ...metricSeries];
});

function normalize(values: (number | null)[]): (number | null)[] {
  const lastValue = values.find((it) => it !== null);
  if (lastValue === undefined || lastValue === 0) return values;
  return values.map((it) => it && it / lastValue);
}

const data = computed<uPlot.AlignedData>(() => {
  if (!graph.data) return [];
  const indices = graph.data.commits.map((_, i) => i);
  const measurements = graph.data.metrics
    .map((it) => it.measurements)
    .map((it) => (queryNormalize.value ? normalize(it) : it));
  return [indices, ...measurements];
});

function openCurrentCommitInNewTab() {
  if (hoverCommit.value === undefined) return;
  const resolved = router.resolve({
    name: "/repos.[repo].commits.[chash]",
    params: { repo: route.params.repo, chash: hoverCommit.value.chash },
  });
  window.open(resolved.href, "_blank");
}
</script>

<template>
  <div class="flex min-h-0 max-w-[2000px] gap-4">
    <div class="flex min-w-[500px] flex-2 flex-col gap-2">
      <div class="flex flex-wrap gap-1">
        <label class="bg-background-alt w-fit p-1 align-baseline select-none" title="Number of recent commits to fetch">
          n:
          <input v-model="queryN" type="number" :min="nMin" :max="nMax" :step="nStep" class="bg-background px-1" />
        </label>
        <label
          class="bg-background-alt w-fit p-1 align-baseline select-none"
          title="Whether the y axis should start at 0 or at the lowest measured value"
        >
          Start at 0:
          <input v-model="queryZero" type="checkbox" />
        </label>
        <label
          class="bg-background-alt w-fit p-1 align-baseline select-none"
          title="Whether the plots should be scaled so they all start at a value of 1"
        >
          Normalize:
          <input v-model="queryNormalize" type="checkbox" />
        </label>
      </div>

      <div class="flex min-h-0 flex-col bg-white text-black" @click.middle="openCurrentCommitInNewTab()">
        <PUplot v-model:hover-idx="hoverIdx" :series :data :start-at-zero="queryZero" class="overflow-y-scroll" />
        <div class="hidden pt-[0.1em] text-center text-[0.6em] dark:block">Sorry for brutzeling your eye balls.</div>
      </div>

      <PCommitInfo
        v-if="hoverCommit"
        :repo="route.params.repo"
        :url="repo?.url"
        :chash="hoverCommit.chash"
        :author="hoverCommit.author"
        :title="hoverCommit.title"
        :body="hoverCommit.body"
        class="border-t pt-1 dark:border-none"
      />
    </div>

    <PMetricSelector
      v-model:filter="queryS"
      v-model:selected="queryM"
      :repo="route.params.repo"
      :limit="metricLimit"
      class="min-w-fit flex-1"
    />
  </div>
</template>
