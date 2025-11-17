<script setup lang="ts">
import { useRepoGraph } from "@/api/repoGraph.ts";
import { useRepo } from "@/api/repos.ts";
import CBrutzelBox from "@/components/CBrutzelBox.vue";
import CCommitDetails from "@/components/CCommitDetails.vue";
import CControl from "@/components/CControl.vue";
import CLoading from "@/components/CLoading.vue";
import PMetricSelector from "@/components/pages/graph/PMetricSelector.vue";
import PUplot from "@/components/pages/graph/PUplot.vue";
import {
  useQueryParamAsBool,
  useQueryParamAsInt,
  useQueryParamAsString,
  useQueryParamAsStringSet,
} from "@/lib/query.ts";
import { parseMetric } from "@/lib/utils.ts";
import type uPlot from "uplot";
import { computed, reactive, ref, watchEffect } from "vue";
import { useRoute, useRouter } from "vue-router";

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
  "#800000",
  "#808000",
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
const queryNormalize = useQueryParamAsString("normalize");
const queryRight = useQueryParamAsString("right");

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

const categories = computed<string[]>(() => {
  const categories = Array.from(queryM.value)
    .map((it) => parseMetric(it)[1])
    .filter((it) => it !== undefined);
  return Array.from(new Set(categories)).sort();
});

// Reset queryRight if it is no longer present in any of the selected metrics
watchEffect(() => {
  if (!queryRight.value) return;
  if (categories.value.includes(queryRight.value)) return;
  queryRight.value = "";
});

function isRightCategory(metric: string): boolean {
  if (queryRight.value === "") return false;
  const category = parseMetric(metric)[1];
  return category === queryRight.value;
}

const series = computed<uPlot.Series[]>(() => {
  const commitSeries: uPlot.Series = {
    label: "n",
  };

  const metricSeries: uPlot.Series[] = Array.from(queryM.value)
    .sort()
    .map((it, i) => ({
      label: it,
      stroke: colors[i % colors.length] ?? "red",
      scale: isRightCategory(it) ? "yright" : "y",
    }));

  return [commitSeries, ...metricSeries];
});

function normalizeAtStart(values: (number | null)[]): (number | null)[] {
  const firstValue = values.filter((it) => it !== null).find((it) => it !== 0);
  if (firstValue === undefined) return values;
  return values.map((it) => it && it / firstValue);
}

function normalizeAtEnd(values: (number | null)[]): (number | null)[] {
  const result = Array.from(values).reverse();
  return normalizeAtStart(result).reverse();
}

function normalize(values: (number | null)[]): (number | null)[] {
  if (queryNormalize.value === "start") return normalizeAtStart(values);
  if (queryNormalize.value === "end") return normalizeAtEnd(values);
  return values;
}

const data = computed<uPlot.AlignedData>(() => {
  if (!graph.data) return [];
  const indices = graph.data.commits.map((_, i) => i);
  const measurements = graph.data.metrics.map((it) => it.measurements).map((it) => normalize(it));
  return [indices, ...measurements];
});

function openCurrentCommitInNewTab() {
  if (hoverCommit.value === undefined) return;
  const resolved = router.resolve({
    name: "/repos.[repo].commits.[chash]",
    params: { repo: route.params.repo, chash: hoverCommit.value.chash },
    query: { s: queryS.value || undefined },
  });
  window.open(resolved.href, "_blank");
}

// Backwards compatibility with previous queryNormalize values
watchEffect(() => {
  if (queryNormalize.value === "true") queryNormalize.value = "start";
  else if (queryNormalize.value === "false") queryNormalize.value = "";
});
</script>

<template>
  <div class="flex min-h-0 max-w-[2000px] gap-4">
    <div class="flex min-w-[500px] flex-2 flex-col gap-2">
      <div class="flex flex-wrap gap-1">
        <CControl>
          <label class="select-none" title="Number of recent commits to fetch">
            n:
            <input v-model="queryN" type="number" :min="nMin" :max="nMax" :step="nStep" class="bg-background px-1" />
          </label>
        </CControl>

        <CControl>
          <label class="select-none" title="Whether the y axis should start at 0 or at the lowest measured value.">
            Start at 0:
            <input v-model="queryZero" type="checkbox" />
          </label>
        </CControl>

        <CControl>
          <label
            class="select-none"
            title="Whether the plots should be scaled so they all start or end at a value of 1."
          >
            Normalize:
            <select v-model="queryNormalize" class="bg-background px-1">
              <option value="">no</option>
              <option value="start">start</option>
              <option value="end">end</option>
            </select>
          </label>
        </CControl>

        <CControl>
          <label
            class="select-none"
            title="Display all metrics with this category (i.e. the part after //) on a separate axis to the right of the graph."
          >
            Right axis:
            <select
              v-model="queryRight"
              :disabled="categories.length === 0"
              class="bg-background disabled:text-foreground-alt px-1"
            >
              <option value="">none</option>
              <hr />
              <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
            </select>
          </label>
        </CControl>
      </div>

      <CLoading v-if="!graph.isSuccess" :error="graph.error" />
      <CBrutzelBox v-show="graph.isSuccess" class="min-h-0" @click.middle="openCurrentCommitInNewTab()">
        <PUplot v-model:hover-idx="hoverIdx" :series :data :start-at-zero="queryZero" class="overflow-y-scroll" />
      </CBrutzelBox>

      <CCommitDetails
        v-if="hoverCommit"
        :repo="route.params.repo"
        :repo-url="repo?.url"
        :commit="hoverCommit"
        :query-s="queryS"
        class="border-t pt-1 dark:border-none"
      />
    </div>

    <PMetricSelector
      v-model:filter="queryS"
      v-model:selected="queryM"
      :repo="route.params.repo"
      class="min-w-fit flex-1"
    />
  </div>
</template>
