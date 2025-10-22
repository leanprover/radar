<script setup lang="ts">
import { postAdminEnqueue } from "@/api/adminEnqueue.ts";
import CButton from "@/components/CButton.vue";
import CLinkCommitHash from "@/components/CLinkCommitHash.vue";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CLoading from "@/components/CLoading.vue";
import CRunInfo from "@/components/CRunInfo.vue";
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CTimeAgo from "@/components/CTimeAgo.vue";
import CTimeInstant from "@/components/CTimeInstant.vue";
import PCommitMessage from "@/components/pages/commit/PCommitMessage.vue";
import PCommitNavChildren from "@/components/pages/commit/PCommitNavChildren.vue";
import PCommitNavParents from "@/components/pages/commit/PCommitNavParents.vue";
import PMeasurementsTable from "@/components/pages/commit/PMeasurementsTable.vue";
import PMessage from "@/components/pages/commit/PMessage.vue";
import { invalidateCommit, useCommit } from "@/composables/useCommit.ts";
import { invalidateCompare, useCompare } from "@/composables/useCompare.ts";
import { useRepo } from "@/composables/useRepo.ts";
import { queryParamAsNonemptyString } from "@/lib/query.ts";
import { setsEqual } from "@/lib/utils.ts";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { useQueryClient } from "@tanstack/vue-query";
import { useIntervalFn } from "@vueuse/core";
import { useRouteQuery } from "@vueuse/router";
import { computed, reactive, ref, watch } from "vue";
import { onBeforeRouteUpdate, useRoute } from "vue-router";

const route = useRoute("/repos.[repo].commits.[chash]");
const admin = useAdminStore();
const queryClient = useQueryClient();

const queryParentRaw = useRouteQuery("parent");
const queryParent = computed(() => queryParamAsNonemptyString(queryParentRaw.value) ?? "parent");

const queryFilterRaw = useRouteQuery("s");
const queryFilter = computed({
  get() {
    return queryParamAsNonemptyString(queryFilterRaw.value);
  },
  set(value) {
    if (value !== undefined) {
      value = value.trim();
      if (value === "") value = undefined;
    }
    queryFilterRaw.value = value;
  },
});

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
    () => queryParent.value,
    () => route.params.chash,
  ),
);
const measurements = computed(() => {
  if (!compare.isSuccess) return undefined;
  const data = compare.data.measurements.filter((it) => it.second !== undefined);
  if (data.length === 0) return undefined;
  return data;
});

const major = computed(() => {
  if (!compare.isSuccess) return [];
  return compare.data.measurements
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => it.major)
    .map((it) => it.message);
});
const minor = computed(() => {
  if (!compare.isSuccess) return [];
  return compare.data.measurements
    .map((it) => it.significance)
    .filter((it) => it !== undefined)
    .filter((it) => !it.major)
    .map((it) => it.message);
});

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

const enqueuePriority = ref(-1);
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
      <PCommitNavParents :repo="route.params.repo" :commits="commit.data.parents" />
      <PCommitNavChildren :repo="route.params.repo" :commits="commit.data.children" />
    </div>
  </CSection>

  <CSection v-if="admin.token !== undefined">
    <CSectionTitle>Admin</CSectionTitle>
    <div class="bg-background-alt flex max-w-[80ch] flex-col gap-2 p-1">
      <div class="flex gap-2">
        <CButton
          @click="
            postAdminEnqueue(admin.token, route.params.repo, route.params.chash, enqueuePriority);
            invalidateCommit(queryClient, route.params.repo, route.params.chash);
            invalidateCompare(queryClient, route.params.repo, queryParent, route.params.chash);
          "
        >
          Enqueue
        </CButton>
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
    <div class="flex flex-col">
      <div v-for="run in commit.data.runs" :key="run.name" class="flex gap-2">
        <div>-</div>
        <CRunInfo :repo="route.params.repo" :chash="route.params.chash" :run />
      </div>
    </div>
  </CSection>

  <CSection v-if="major.length > 0 || minor.length > 0">
    <CSectionTitle>Significant changes</CSectionTitle>
    <details v-if="major.length > 0">
      <summary>Major ({{ major.length }})</summary>
      <CList class="mt-2">
        <CListItem v-for="(msg, i) in major" :key="i">
          <PMessage :segments="msg" />
        </CListItem>
      </CList>
    </details>
    <details v-if="minor.length > 0">
      <summary>Minor ({{ minor.length }})</summary>
      <CList class="mt-2">
        <CListItem v-for="(msg, i) in minor" :key="i">
          <PMessage :segments="msg" />
        </CListItem>
      </CList>
    </details>
  </CSection>

  <CLoading v-if="!compare.isSuccess" :error="compare.error" />
  <CSection v-else-if="measurements !== undefined">
    <CSectionTitle>Measurements</CSectionTitle>
    <PMeasurementsTable v-model:filter="queryFilter" :repo="route.params.repo" :measurements="measurements" />
  </CSection>
</template>
