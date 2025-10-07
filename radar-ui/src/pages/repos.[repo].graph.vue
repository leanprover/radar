<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { useRouteQuery } from "@vueuse/router";
import { computed, reactive } from "vue";
import { queryParamAsNonemptyStringArray } from "@/lib/utils.ts";
import UplotVue from "uplot-vue";
import { useRepoGraph } from "@/composables/useRepoGraph.ts";
import { useRoute } from "vue-router";
import CLoading from "@/components/CLoading.vue";
import { JsonGet } from "@/api/repoGraph.ts";

const route = useRoute("/repos.[repo].graph");

const queryMRaw = useRouteQuery("m");
const queryM = computed({
  get() {
    let rawMetrics = queryParamAsNonemptyStringArray(queryMRaw.value);
    return Array.from(new Set(rawMetrics)).sort();
  },
  set(value) {
    queryMRaw.value = Array.from(new Set(value)).sort();
  },
});

const graph = reactive(useRepoGraph(() => route.params.repo, queryM, 1000));

function mkOptions(data: JsonGet) {
  const series = data.metrics.map((it) => ({
    label: it.metric,
    stroke: "red",
  }));

  return {
    width: 600,
    height: 400,
    series: [{}].concat(series),
  };
}

function mkData(data: JsonGet) {
  const indices: (number | null)[] = data.chashes.map((_, i) => i);
  const measurements: (number | null)[][] = data.metrics.map((it) => it.measurements);
  return [indices].concat(measurements);
}
</script>

<template>
  <CSection>
    <CSectionTitle>Graph</CSectionTitle>
    <CLoading v-if="!graph.isSuccess" :error="graph.error" />
    <UplotVue v-else :options="mkOptions(graph.data)" :data="mkData(graph.data)"></UplotVue>
  </CSection>
</template>
