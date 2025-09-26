<script setup lang="ts">
import { computed, reactive } from "vue";
import { useQueue } from "@/composables/useQueue.ts";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CLoading from "@/components/CLoading.vue";
import { Temporal } from "temporal-polyfill";
import type { JsonTask } from "@/api/queue.ts";
import { type TaskWithActiveRun } from "@/components/pages/queue/PQueueTask.vue";
import PQueue from "@/components/pages/queue/PQueue.vue";
import CTimeAgo from "@/components/CTimeAgo.vue";

const queue = reactive(useQueue());

function runId(repo: string, chash: string, name: string): string {
  return JSON.stringify([repo, chash, name]);
}

const activeRuns = computed(() => {
  const result = new Map<string, Temporal.Instant>();
  if (!queue.isSuccess) return result;
  for (const runner of queue.data.runners) {
    if (runner.activeRun === undefined) continue;
    const id = runId(runner.activeRun.repo, runner.activeRun.chash, runner.activeRun.name);
    result.set(id, runner.activeRun.startTime);
  }
  return result;
});

function tasksWithActiveRun(tasks: JsonTask[]): TaskWithActiveRun[] {
  return tasks.map((task) => ({
    repo: task.repo,
    chash: task.chash,
    title: task.title,
    runs: task.runs.map((run) => ({
      name: run.name,
      runner: run.runner,
      result: run.result,
      active: activeRuns.value.get(runId(task.repo, task.chash, run.name)),
    })),
  }));
}
</script>

<template>
  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <div v-else class="flex flex-col">
    <CSectionTitle>Runners</CSectionTitle>
    <div v-for="runner in queue.data.runners" :key="runner.name" class="flex items-baseline gap-2">
      <div>-</div>
      <div>{{ runner.name }}</div>
      <div class="text-foreground-alt text-xs">
        <template v-if="runner.connected">(connected)</template>
        <template v-else-if="runner.lastSeen">
          (last seen <CTimeAgo :when="runner.lastSeen" class="hover:text-foreground" />)
        </template>
        <template v-else>(never seen)</template>
      </div>
    </div>
  </div>

  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <PQueue v-else :tasks="tasksWithActiveRun(queue.data.tasks)" />
</template>
