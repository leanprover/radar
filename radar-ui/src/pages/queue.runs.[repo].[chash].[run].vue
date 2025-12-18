<script setup lang="ts">
import { useQueueRun } from "@/api/queueRun.ts";
import { useRepo } from "@/api/repos.ts";
import CLoading from "@/components/CLoading.vue";
import CLogs from "@/components/CLogs.vue";
import CSection from "@/components/CSection.vue";
import CTimeDurationSince from "@/components/format/CTimeDurationSince.vue";
import CLinkCommitHash from "@/components/link/CLinkCommitHash.vue";
import CLinkRepo from "@/components/link/CLinkRepo.vue";
import { reactive, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

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
  <CSection v-else :title="`Run ${route.params.run}`">
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

  <CSection v-if="run.isSuccess && run.data !== 'not found'" title="Logs">
    <CLogs
      v-if="run.data.activeRun !== undefined"
      :start-time="run.data.activeRun.startTime"
      :start-line="run.data.activeRun.lines.start"
      :lines="run.data.activeRun.lines.lines"
    />
    <div v-else>No logs available.</div>
  </CSection>
</template>
