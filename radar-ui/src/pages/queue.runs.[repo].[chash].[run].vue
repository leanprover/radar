<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { reactive, watch } from "vue";
import CLoading from "@/components/CLoading.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { formatZonedTime, instantToZoned } from "@/lib/utils.ts";
import CCommitHash from "@/components/CCommitHash.vue";
import { useRepo } from "@/composables/useRepo.ts";
import { useQueueRun } from "@/composables/useQueueRun.ts";
import CTimeDurationSince from "@/components/CTimeDurationSince.vue";

const router = useRouter();
const route = useRoute("/repos.[repo].commits.[chash].runs.[run]");

const repo = useRepo(route.params.repo);
const run = reactive(
  useQueueRun(
    () => route.params.repo,
    () => route.params.chash,
    () => route.params.run,
  ),
);

// Redirect to finished run page once done
watch(run, (newValue) => {
  if (!newValue.isSuccess) return;
  if (newValue.data === "not found")
    void router.push({ name: "/repos.[repo].commits.[chash].runs.[run]", params: route.params });
});
</script>

<template>
  <CLoading v-if="!run.isSuccess" :error="run.error" />
  <div v-else-if="run.data === 'not found'" class="text-foreground-alt">Redirecting...</div>
  <div v-else class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
    <CSectionTitle class="col-span-full">Run</CSectionTitle>

    <div>Runner:</div>
    <div>{{ run.data.runner }}</div>

    <div>Script:</div>
    <div>{{ run.data.script }}</div>

    <template v-if="run.data.activeRun">
      <div>Bench commit:</div>
      <CCommitHash :url="repo?.benchUrl" :chash="run.data.activeRun.benchChash" />

      <div>Duration:</div>
      <CTimeDurationSince :start="run.data.activeRun.startTime" />
    </template>

    <div v-if="run.data.activeRun" class="col-span-full mt-2">Currently being executed...</div>
    <div v-else class="col-span-full mt-2">Currently awaiting runner...</div>
  </div>

  <div v-if="run.isSuccess && run.data !== 'not found'" class="flex flex-col">
    <CSectionTitle>Logs</CSectionTitle>
    <div v-if="run.data.activeRun === undefined">No logs available.</div>
    <template v-else>
      <div class="mb-2">Lines: {{ run.data.activeRun.lines.start + run.data.activeRun.lines.lines.length }}</div>
      <div class="flex flex-col">
        <span
          v-for="(line, index) in run.data.activeRun.lines.lines"
          :key="index"
          :class="['whitespace-pre-wrap', { 'text-red': line.source === 1, 'text-blue': line.source === 2 }]"
          >[{{ formatZonedTime(instantToZoned(line.time)) }}] {{ line.line }}</span
        >
      </div>
    </template>
  </div>
</template>
