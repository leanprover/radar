<script setup lang="ts">
import PQueueTaskRun, { type Run } from "@/components/pages/queue/PQueueTaskRun.vue";

export interface Task {
  repo: string;
  chash: string;
  title: string;
  runs: Run[];
}

const { repo, chash, title, runs } = defineProps<Task>();
</script>

<template>
  <div class="flex gap-2">
    <div>-</div>
    <div class="flex flex-col">
      <div>
        <RouterLink
          :to="{ name: '/repos.[repo]', params: { repo } }"
          class="text-foreground-alt italic hover:underline"
        >
          {{ repo }}
        </RouterLink>
        {{ " " }}
        <RouterLink :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash } }" class="hover:underline">
          {{ title }}
        </RouterLink>
      </div>

      <PQueueTaskRun v-for="run in runs" :key="run.name" v-bind="run" />
    </div>
  </div>
</template>
