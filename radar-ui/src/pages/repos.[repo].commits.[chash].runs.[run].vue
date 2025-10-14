<script setup lang="ts">
import CLinkCommitHash from "@/components/CLinkCommitHash.vue";
import CLinkRepo from "@/components/CLinkRepo.vue";
import CLoading from "@/components/CLoading.vue";
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CTimeDurationBetween from "@/components/CTimeDurationBetween.vue";
import CTimeRange from "@/components/CTimeRange.vue";
import { useCommitRun } from "@/composables/useCommitRun.ts";
import { useRepo } from "@/composables/useRepo.ts";
import { formatZonedTime, instantToZoned } from "@/lib/format.ts";
import { reactive } from "vue";
import { useRoute } from "vue-router";

const route = useRoute("/repos.[repo].commits.[chash].runs.[run]");

const repo = useRepo(route.params.repo);
const run = reactive(
  useCommitRun(
    () => route.params.repo,
    () => route.params.chash,
    () => route.params.run,
  ),
);
</script>

<template>
  <CLoading v-if="!run.isSuccess" :error="run.error" />
  <CSection v-else>
    <CSectionTitle>Run {{ route.params.run }}</CSectionTitle>

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

  <CSection v-if="run.isSuccess">
    <CSectionTitle>Logs</CSectionTitle>
    <div v-if="run.data.lines === undefined">No logs available.</div>
    <template v-else>
      <div>Lines: {{ run.data.lines.length }}</div>
      <div class="flex flex-col">
        <span
          v-for="(line, index) in run.data.lines"
          :key="index"
          :class="['whitespace-pre-wrap', { 'text-red': line.source === 1, 'text-blue': line.source === 2 }]"
          >[{{ formatZonedTime(instantToZoned(line.time)) }}] {{ line.line }}</span
        >
      </div>
    </template>
  </CSection>
</template>
