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
import PCommitNavChildren from "@/components/pages/commit/PCommitNavChildren.vue";
import PCommitNavParents from "@/components/pages/commit/PCommitNavParents.vue";
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

const tick = useIntervalFn(onTick, 5000);
function onTick() {
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
}

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

onBeforeRouteUpdate(() => {
  tick.resume();
});
</script>

<template>
  <CLoading v-if="!commit.isSuccess" :error="commit.error" />
  <CSection v-else title="Commit">
    <CCommitDetails :repo-url="repo?.url" :commit="commit.data.commit" />

    <div class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
      <a
        v-if="repo?.lakeprofReportUrl"
        :href="repo.lakeprofReportUrl + route.params.chash"
        target="_blank"
        class="col-span-full hover:underline"
      >
        Lakeprof report
      </a>

      <PCommitNavParents :repo="route.params.repo" :search="queryFilter" :commits="commit.data.parents" />
      <PCommitNavChildren :repo="route.params.repo" :search="queryFilter" :commits="commit.data.children" />
    </div>
  </CSection>

  <CSection v-if="admin.token !== undefined" title="Admin" collapsible>
    <PFormEnqueue :repo="route.params.repo" :chash="route.params.chash" :reference />
  </CSection>

  <CSection v-if="commit.isSuccess && commit.data.runs.length > 0" title="Runs">
    <CList>
      <CListItem v-for="run in commit.data.runs" :key="run.name">
        <CRunInfo :repo="route.params.repo" :chash="route.params.chash" :run />
      </CListItem>
    </CList>
  </CSection>

  <CSection v-if="details.major > 0 || details.minor > 0" title="Significant details" collapsible start-open>
    <PDetailsSection title="Runs" :messages="details.runs" open />
    <PDetailsSection title="Major changes" :messages="details.metricsMajor" open />
    <PDetailsSection title="Minor changes" :messages="details.metricsMinor" open />
  </CSection>

  <CSection v-show="measurements.length > 0" title="Measurements">
    <PMeasurementsTable v-model:filter="queryFilter" :repo="route.params.repo" :measurements="measurements" />
  </CSection>
</template>
