<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { computed, reactive, ref } from "vue";
import { useQueryParamAsBool, useQueryParamAsInt, useQueryParamAsStringArray } from "@/lib/utils.ts";
import UplotVue from "uplot-vue";
import { useRepoGraph } from "@/composables/useRepoGraph.ts";
import { useRoute } from "vue-router";
import CLoading from "@/components/CLoading.vue";
import type uPlot from "uplot";
import { formatValue } from "@/lib/format.ts";
import CLinkCommitHash from "@/components/CLinkCommitHash.vue";
import { useRepo } from "@/composables/useRepo.ts";
import CTimeAgo from "@/components/CTimeAgo.vue";
import CTimeInstant from "@/components/CTimeInstant.vue";
import PCommitMessage from "@/components/pages/commit/PCommitMessage.vue";

const route = useRoute("/repos.[repo].graph");
const repo = useRepo(route.params.repo);

const queryM = useQueryParamAsStringArray("m", { deduplicate: true, sort: true });
const queryN = useQueryParamAsInt("n", 100, { min: 100, max: 1000 });
const queryZero = useQueryParamAsBool("zero", true);

const graph = reactive(useRepoGraph(() => route.params.repo, queryM, queryN));

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

const data = computed<uPlot.AlignedData>(() => {
  if (!graph.data) return [[]];
  const indices = graph.data.commits.map((_, i) => i);
  const measurements = graph.data.metrics.map((it) => it.measurements);
  return [indices, ...measurements];
});
</script>

<template>
  <CSection>
    <CSectionTitle>Graph</CSectionTitle>

    <div class="flex flex-wrap gap-1">
      <label class="bg-background-alt w-fit p-1 align-baseline select-none">
        n:
        <input v-model="queryN" type="number" min="100" max="1000" step="100" class="bg-background px-1" />
      </label>
      <label class="bg-background-alt w-fit p-1 align-baseline select-none">
        Start at 0:
        <input v-model="queryZero" type="checkbox" />
      </label>
    </div>

    <div class="flex flex-wrap items-start gap-8">
      <CLoading v-if="!graph.isSuccess" :error="graph.error" />
      <UplotVue key="graph" :options :data />

      <div v-if="hoverCommit" class="grid w-[80ch] grid-cols-[auto_1fr] gap-x-2">
        <CLinkCommitHash
          :repo="route.params.repo"
          :url="repo?.url"
          :chash="hoverCommit.chash"
          class="text-yellow col-span-full"
        />

        <div>Author:</div>
        <div>{{ hoverCommit.author.name }} &lt;{{ hoverCommit.author.email }}&gt;</div>

        <div>Date:</div>
        <div>
          <CTimeInstant :when="hoverCommit.author.time" />
          <span class="text-foreground-alt text-xs"> (<CTimeAgo :when="hoverCommit.author.time" />)</span>
        </div>

        <PCommitMessage :title="hoverCommit.title" :body="hoverCommit.body" />
      </div>
    </div>
  </CSection>
</template>
