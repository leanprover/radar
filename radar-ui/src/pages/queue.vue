<script setup lang="ts">
import { computed, reactive } from "vue";
import { useQueue } from "@/composables/useQueue.ts";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CLoading from "@/components/CLoading.vue";
import { cn } from "@/lib/utils.ts";

const queue = reactive(useQueue());

function runStr(repo: string, chash: string, script: string): string {
  return JSON.stringify([repo, chash, script]);
}

const runStates = computed(() => {
  const result = new Map<string, "running" | "success" | "error">();
  if (!queue.isSuccess) return result;

  // Read state from tasks
  for (const task of queue.data.tasks) {
    for (const run of task.runs) {
      const str = runStr(task.repo, task.chash, run.script);
      if (run.exitCode === 0) result.set(str, "success");
      else if (run.exitCode !== null) result.set(str, "error");
    }
  }

  // Update state with runner statuses
  for (const runner of queue.data.runners) {
    if (runner.activeRun === null) continue;
    const str = runStr(runner.activeRun.repo, runner.activeRun.chash, runner.activeRun.script);
    result.set(str, "running");
  }

  return result;
});

function runsWithState(task: {
  repo: string;
  chash: string;
  runs: { name: string; script: string; runner: string }[];
}): { name: string; script: string; runner: string; state: "ready" | "running" | "success" | "error" }[] {
  return task.runs.map((run) => {
    const str = runStr(task.repo, task.chash, run.script);
    const state = runStates.value.get(str) ?? "ready";
    return { name: run.name, script: run.script, runner: run.runner, state };
  });
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
          (last seen
          <span
            :title="useDateFormat(runner.lastSeen.epochMilliseconds, 'YYYY-MM-DD HH:mm:ss').value"
            class="hover:text-foreground"
            >{{ useTimeAgo(runner.lastSeen.epochMilliseconds) }}</span
          >)
        </template>
        <template v-else>(never seen)</template>
      </div>
    </div>
  </div>

  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <div v-else class="flex flex-col">
    <CSectionTitle>Queue</CSectionTitle>
    <div v-if="queue.data.tasks.length === 0" class="text-foreground-alt">empty \o/</div>
    <div v-else class="flex flex-col gap-2">
      <div v-for="task in queue.data.tasks" :key="JSON.stringify([task.repo, task.chash])" class="flex gap-2">
        <div>-</div>
        <div class="flex flex-col">
          <div class="flex gap-2">
            <RouterLink
              :to="{ name: '/repos.[repo]', params: { repo: task.repo } }"
              class="text-foreground-alt italic hover:underline"
            >
              {{ task.repo }}
            </RouterLink>
            <RouterLink
              :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: task.repo, chash: task.chash } }"
              class="hover:underline"
            >
              {{ task.title }}
            </RouterLink>
          </div>
          <div
            v-for="run in runsWithState(task)"
            :key="run.name"
            :class="
              cn('text-xs', {
                'text-foreground-alt': run.state === 'ready',
                'text-blue': run.state === 'running',
                'text-green': run.state === 'success',
                'text-red': run.state === 'error',
              })
            "
          >
            <span :title="run.script" class="hover:text-foreground">{{ run.name }}</span>
            ({{ run.runner }}):
            <span>{{ run.state }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
