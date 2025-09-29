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
import CSection from "@/components/CSection.vue";

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
  <CSection v-else>
    <CSectionTitle class="col-span-full">Run</CSectionTitle>

    <div class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
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
    </div>

    <div v-if="run.data.activeRun" class="col-span-full">Currently being executed...</div>
    <div v-else class="col-span-full">Currently awaiting runner...</div>
  </CSection>

  <CSection v-if="run.isSuccess && run.data !== 'not found'">
    <CSectionTitle>Logs</CSectionTitle>
    <div v-if="run.data.activeRun === undefined">No logs available.</div>
    <template v-else>
      <div>Lines: {{ run.data.activeRun.lines.start + run.data.activeRun.lines.lines.length }}</div>
      <div class="flex flex-col">
        <span
          v-for="(line, index) in run.data.activeRun.lines.lines"
          :key="index"
          :class="['whitespace-pre-wrap', { 'text-red': line.source === 1, 'text-blue': line.source === 2 }]"
          >[{{ formatZonedTime(instantToZoned(line.time)) }}] {{ line.line }}</span
        >
      </div>
    </template>
  </CSection>
</template>
