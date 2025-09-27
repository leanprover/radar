<script setup lang="ts">
import { useRoute } from "vue-router";
import { reactive } from "vue";
import CLoading from "@/components/CLoading.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { useCommitRun } from "@/composables/useCommitRun.ts";
import CTimeInstant from "@/components/CTimeInstant.vue";
import CTimeDurationBetween from "@/components/CTimeDurationBetween.vue";
import CTimeRange from "@/components/CTimeRange.vue";

const route = useRoute("/repos.[repo].commits.[chash].runs.[run]");

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
  <div v-else class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
    <CSectionTitle class="col-span-full">Run</CSectionTitle>

    <div>Runner:</div>
    <div>{{ run.data.runner }}</div>

    <div>Script:</div>
    <div>{{ run.data.script }}</div>

    <!-- TODO Link to github -->
    <div>Bench commit:</div>
    <div>{{ run.data.benchChash }}</div>

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
  </div>

  <div v-if="run.isSuccess" class="flex flex-col">
    <CSectionTitle>Logs</CSectionTitle>
    <div v-if="run.data.lines === undefined">No logs available.</div>
    <template v-else>
      <div class="mb-2">Lines: {{ run.data.lines.length }}</div>
      <div class="flex flex-col">
        <div
          v-for="(line, index) in run.data.lines"
          :key="index"
          :class="{ 'text-red': line.source === 1, 'text-blue': line.source === 2 }"
        >
          [<CTimeInstant :when="line.time" :date="false" />]
          {{ line.line }}
        </div>
      </div>
    </template>
  </div>
</template>
