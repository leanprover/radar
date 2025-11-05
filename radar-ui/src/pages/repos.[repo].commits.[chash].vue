<script setup lang="ts">
import { useCommit } from "@/api/commit.ts";
import { useCompare } from "@/api/compare.ts";
import { useRepo } from "@/api/repos.ts";
import CCommitDetails from "@/components/CCommitDetails.vue";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CLoading from "@/components/CLoading.vue";
import CRunInfo from "@/components/CRunInfo.vue";
import CSection from "@/components/CSection.vue";
import PCommitNav from "@/components/pages/commit/PCommitNav.vue";
import PDetailsSection from "@/components/pages/commit/PDetailsSection.vue";
import PFormEnqueue from "@/components/pages/commit/PFormEnqueue.vue";
import PMeasurementsTable, { type Measurement } from "@/components/pages/commit/PMeasurementsTable.vue";
import { comparisonSignificance } from "@/components/pages/commit/significance.ts";
import { useQueryParamAsString } from "@/lib/query.ts";
import { setsEqual } from "@/lib/utils.ts";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { useIntervalFn } from "@vueuse/core";
import { computed, reactive, watch } from "vue";
import { onBeforeRouteUpdate, useRoute } from "vue-router";

const route = useRoute("/repos.[repo].commits.[chash]");
const admin = useAdminStore();

const queryParent = useQueryParamAsString("parent");
const queryFilter = useQueryParamAsString("s");

const reference = computed(() => queryParent.value || "parent");

const repo = useRepo(route.params.repo);
const commit = reactive(
  useCommit(
    () => route.params.repo,
    () => route.params.chash,
  ),
);
const compare = reactive(
  useCompare(
    () => route.params.repo,
    reference,
    () => route.params.chash,
  ),
);

const measurements = computed<Measurement[]>(() => {
  if (!compare.isSuccess) return [];
  return compare.data.comparison.metrics.filter((it) => it.second !== undefined);
});

const details = computed(() => comparisonSignificance(compare.data?.comparison));

// Regularly refetch the commit info (which includes the finished and unfinished runs) until all runs are finished.
const tick = useIntervalFn(() => {
  if (!commit.isSuccess) return;

  if (commit.data.runs.every((it) => it.finished)) {
    // We've already fetched this data plus the corresponding measurement data the last time around,
    // so there's no need to re-fetch or re-check again.
    // Pausing this interval doesn't affect correctness,
    // but there's little point in re-running the function until something changes.
    tick.pause();
    return;
  }

  void commit.refetch();
}, 5000);
onBeforeRouteUpdate(() => {
  tick.resume();
});

// Re-fetch the measurement data every time a new run is completed.
const completedRuns = computed(() => {
  const result = new Set<string>();
  if (commit.isSuccess) for (const run of commit.data.runs) if (run.finished) result.add(run.name);
  return result;
});
watch(completedRuns, (newValue, oldValue) => {
  if (setsEqual(newValue, oldValue)) return; // No new runs
  void compare.refetch();
});
</script>

<template>
  <CSection title="Commit">
    <CLoading v-if="!commit.isSuccess" :error="commit.error" />
    <template v-else>
      <CCommitDetails :repo-url="repo?.url" :commit="commit.data.commit" />
      <PCommitNav
        :repo="route.params.repo"
        :chash="route.params.chash"
        :search="queryFilter"
        :lakeprof-report-url="repo?.lakeprofReportUrl"
        :parents="commit.data.parents"
        :children="commit.data.children"
      />
    </template>
  </CSection>

  <CSection v-if="admin.token !== undefined" title="Admin" collapsible>
    <PFormEnqueue :repo="route.params.repo" :chash="route.params.chash" :reference />
  </CSection>

  <CSection title="Runs">
    <CLoading v-if="!commit.isSuccess" :error="commit.error" />
    <CList v-else>
      <div v-if="commit.data.runs.length === 0">No runs.</div>
      <CListItem v-for="run in commit.data.runs" :key="run.name">
        <CRunInfo :repo="route.params.repo" :chash="route.params.chash" :run />
      </CListItem>
    </CList>
  </CSection>

  <CSection title="Significant results" collapsible start-open>
    <CLoading v-if="!compare.isSuccess" :error="compare.error" />
    <div v-else-if="details.major === 0 && details.minor === 0">No significant results.</div>
    <template v-else>
      <PDetailsSection title="Runs" :messages="details.runs" open />
      <PDetailsSection title="Major changes" :messages="details.metricsMajor" open />
      <PDetailsSection title="Minor changes" :messages="details.metricsMinor" open />
    </template>
  </CSection>

  <CSection title="Measurements">
    <PMeasurementsTable v-model:filter="queryFilter" :repo="route.params.repo" :measurements="measurements" />
    <CLoading v-if="!compare.isSuccess" :error="compare.error" />
    <div v-else-if="measurements.length === 0">No measurements.</div>
  </CSection>
</template>
