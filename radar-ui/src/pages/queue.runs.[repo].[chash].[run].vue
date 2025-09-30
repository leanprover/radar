<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { reactive, watch } from "vue";
import CLoading from "@/components/CLoading.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { formatZonedTime, instantToZoned } from "@/lib/utils.ts";
import CLinkCommitHash from "@/components/CLinkCommitHash.vue";
import { useRepo } from "@/composables/useRepo.ts";
import { useQueueRun } from "@/composables/useQueueRun.ts";
import CTimeDurationSince from "@/components/CTimeDurationSince.vue";
import CSection from "@/components/CSection.vue";
import CLinkRepo from "@/components/CLinkRepo.vue";

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
    <CSectionTitle class="col-span-full">Run {{ route.params.run }}</CSectionTitle>

    <div class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
      <div>Repo:</div>
      <CLinkRepo :repo="route.params.repo" />

      <div>Commit:</div>
      <CLinkCommitHash :repo="route.params.repo" :url="repo?.url" :chash="route.params.chash" />

      <template v-if="run.data.activeRun">
        <div>Bench commit:</div>
        <CLinkCommitHash :url="repo?.benchUrl" :chash="run.data.activeRun.benchChash" />
      </template>

      <div>Runner:</div>
      <div>{{ run.data.runner }}</div>

      <div>Script:</div>
      <div>{{ run.data.script }}</div>

      <template v-if="run.data.activeRun">
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
