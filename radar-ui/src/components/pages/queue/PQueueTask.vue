<script setup lang="ts">
import CRunInfo, { type Run } from "@/components/CRunInfo.vue";

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

      <CRunInfo v-for="run in runs" :key="run.name" :repo :chash :run small green />
    </div>
  </div>
</template>
