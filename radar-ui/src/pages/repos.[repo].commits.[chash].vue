<script setup lang="ts">
import { postAdminEnqueue } from "@/api/adminEnqueue.ts";
import { invalidateCommit, useCommit } from "@/api/commit.ts";
import { invalidateCompare, useCompare } from "@/api/compare.ts";
import { useRepo } from "@/api/repos.ts";
import type { JsonMessageSegment } from "@/api/types.ts";
import CButton from "@/components/CButton.vue";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CLoading from "@/components/CLoading.vue";
import CRunInfo from "@/components/CRunInfo.vue";
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CTimeAgo from "@/components/format/CTimeAgo.vue";
import CTimeInstant from "@/components/format/CTimeInstant.vue";
import CLinkCommitHash from "@/components/link/CLinkCommitHash.vue";
import PCommitMessage from "@/components/pages/commit/PCommitMessage.vue";
import PCommitNavChildren from "@/components/pages/commit/PCommitNavChildren.vue";
import PCommitNavParents from "@/components/pages/commit/PCommitNavParents.vue";
import PComparisonSection from "@/components/pages/commit/PComparisonSection.vue";
import PMeasurementsTable, { type Measurement } from "@/components/pages/commit/PMeasurementsTable.vue";
import { useQueryParamAsString } from "@/lib/query.ts";
import { setsEqual } from "@/lib/utils.ts";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { useQueryClient } from "@tanstack/vue-query";
import { useIntervalFn } from "@vueuse/core";
import { computed, reactive, ref, watch } from "vue";
import { onBeforeRouteUpdate, useRoute } from "vue-router";

const route = useRoute("/repos.[repo].commits.[chash]");
const admin = useAdminStore();
const queryClient = useQueryClient();

const queryParent = useQueryParamAsString("parent");
const queryFilter = useQueryParamAsString("s");

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
    () => queryParent.value || "parent",
    () => route.params.chash,
  ),
);
const measurements = computed<Measurement[]>(() => {
  if (!compare.isSuccess) return [];
  return compare.data.comparison.metrics.filter((it) => it.second !== undefined);
});

const significantRuns = computed<JsonMessageSegment[][]>(() => {
  if (!compare.isSuccess) return [];
  return compare.data.comparison.runs
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .map((it) => it.message);
});
const significantMajorMetrics = computed<JsonMessageSegment[][]>(() => {
  if (!compare.isSuccess) return [];
  return compare.data.comparison.metrics
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => it.major)
    .map((it) => it.message);
});
const significantMinorMetrics = computed<JsonMessageSegment[][]>(() => {
  if (!compare.isSuccess) return [];
  return compare.data.comparison.metrics
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => !it.major)
    .map((it) => it.message);
});

const enqueuePriority = ref(-1);

async function onEnqueue() {
  if (admin.token === undefined) return;
  await postAdminEnqueue(admin.token, route.params.repo, route.params.chash, enqueuePriority.value);
  void invalidateCommit(queryClient, route.params.repo, route.params.chash);
  void invalidateCompare(queryClient, route.params.repo, queryParent.value, route.params.chash);
}

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
  <CSection v-else>
    <CSectionTitle>Commit</CSectionTitle>

    <div class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
      <div class="text-yellow col-span-full">
        commit <CLinkCommitHash :url="repo?.url" :chash="route.params.chash" />
      </div>

      <div>Author:</div>
      <div>{{ commit.data.commit.author.name }} &lt;{{ commit.data.commit.author.email }}&gt;</div>

      <div>Date:</div>
      <div>
        <CTimeInstant :when="commit.data.commit.author.time" />
        <span class="text-foreground-alt text-xs"> (<CTimeAgo :when="commit.data.commit.author.time" />)</span>
      </div>

      <template v-if="repo?.lakeprofReportUrl">
        <div>Lakeprof:</div>
        <a :href="repo.lakeprofReportUrl + route.params.chash" target="_blank" class="hover:underline">Report</a>
      </template>

      <PCommitMessage :title="commit.data.commit.title" :body="commit.data.commit.body" class="my-3" />
      <PCommitNavParents :repo="route.params.repo" :search="queryFilter" :commits="commit.data.parents" />
      <PCommitNavChildren :repo="route.params.repo" :search="queryFilter" :commits="commit.data.children" />
    </div>
  </CSection>

  <CSection v-if="admin.token !== undefined">
    <CSectionTitle>Admin</CSectionTitle>
    <div class="bg-background-alt flex max-w-[80ch] flex-col gap-2 p-1">
      <div class="flex gap-2">
        <CButton @click="onEnqueue()"> Enqueue </CButton>
        with priority <input v-model="enqueuePriority" type="number" class="bg-background w-[8ch] px-1" />
      </div>
      <details>
        <summary>Explanation</summary>
        <div class="mt-2">
          Add this commit to the queue (again). Any existing measurement and run data will be deleted. Commits with
          higher priority value appear earlier in the queue. Within a priority, the queue is FIFO.
        </div>
        <div class="mt-2">
          Priority of new commits: 0 <br />
          Priority of commits added by !bench: 1
        </div>
      </details>
    </div>
  </CSection>

  <CSection v-if="commit.isSuccess && commit.data.runs.length > 0">
    <CSectionTitle>Runs</CSectionTitle>
    <CList>
      <CListItem v-for="run in commit.data.runs" :key="run.name">
        <CRunInfo :repo="route.params.repo" :chash="route.params.chash" :run />
      </CListItem>
    </CList>
  </CSection>

  <CSection
    v-if="significantRuns.length > 0 || significantMajorMetrics.length > 0 || significantMinorMetrics.length > 0"
  >
    <CSectionTitle>Significant details</CSectionTitle>
    <PComparisonSection title="Runs" :messages="significantRuns" open />
    <PComparisonSection title="Major changes" :messages="significantMajorMetrics" open />
    <PComparisonSection title="Minor changes" :messages="significantMinorMetrics" />
  </CSection>

  <CSection v-show="measurements.length > 0">
    <CSectionTitle>Measurements</CSectionTitle>
    <PMeasurementsTable v-model:filter="queryFilter" :repo="route.params.repo" :measurements="measurements" />
  </CSection>
</template>
