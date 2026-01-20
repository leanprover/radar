<script setup lang="ts">
import { useCommit } from "@/api/commit.ts";
import { useCommitRun } from "@/api/commitRun.ts";
import { useRepo } from "@/api/repos.ts";
import CLoading from "@/components/CLoading.vue";
import CLogs from "@/components/CLogs.vue";
import CSection from "@/components/CSection.vue";
import CTimeDurationBetween from "@/components/format/CTimeDurationBetween.vue";
import CTimeRange from "@/components/format/CTimeRange.vue";
import CLinkCommitHash from "@/components/link/CLinkCommitHash.vue";
import CLinkRepo from "@/components/link/CLinkRepo.vue";
import { radarTitle } from "@/lib/utils.ts";
import { useTitle } from "@vueuse/core";
import { reactive } from "vue";
import { useRoute } from "vue-router";

const route = useRoute("/repos.[repo].commits.[chash].runs.[run]");

const repo = useRepo(route.params.repo);
const commit = reactive(
  useCommit(
    () => route.params.repo,
    () => route.params.chash,
  ),
);
const run = reactive(
  useCommitRun(
    () => route.params.repo,
    () => route.params.chash,
    () => route.params.run,
  ),
);

useTitle(() =>
  radarTitle(
    `${route.params.run} on ${run.data?.runner ?? "???"}`,
    commit.data?.commit.title ?? route.params.chash,
    route.params.repo,
  ),
);
</script>

<template>
  <CLoading v-if="!run.isSuccess" :error="run.error" />
  <CSection v-else :title="`Run ${route.params.run}`">
    <div class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
      <div>Repo:</div>
      <CLinkRepo :repo="route.params.repo" />

      <div>Commit:</div>
      <CLinkCommitHash :repo="route.params.repo" :url="repo?.url" :chash="route.params.chash" />

      <div>Bench commit:</div>
      <CLinkCommitHash :url="repo?.benchUrl" :chash="run.data.benchChash" />

      <div>Runner:</div>
      <div>{{ run.data.runner }}</div>

      <div>Script:</div>
      <div>{{ run.data.script }}</div>

      <div>Duration:</div>
      <div>
        <CTimeDurationBetween :start="run.data.startTime" :end="run.data.endTime" />
        <span class="text-foreground-alt text-xs">
          (<CTimeRange :start="run.data.startTime" :end="run.data.endTime" />)</span
        >
      </div>

      <template v-if="run.data.scriptStartTime && run.data.scriptEndTime">
        <div>Script duration:</div>
        <div>
          <CTimeDurationBetween :start="run.data.scriptStartTime" :end="run.data.scriptEndTime" />
          {{ " " }}
          <span class="text-foreground-alt text-xs"
            >(<CTimeRange :start="run.data.scriptStartTime" :end="run.data.scriptEndTime" />)</span
          >
        </div>
      </template>

      <div>Exit code:</div>
      <div>{{ run.data.exitCode }}</div>
    </div>
  </CSection>

  <CSection v-if="run.isSuccess" title="Logs">
    <CLogs
      v-if="run.data.lines !== undefined"
      :start-time="run.data.startTime"
      :start-line="0"
      :lines="run.data.lines"
    />
    <div v-else>No logs available.</div>
  </CSection>
</template>
