<script setup lang="ts">
import CLinkCommit from "@/components/CLinkCommit.vue";
import CLinkRepo from "@/components/CLinkRepo.vue";
import CRunInfo, { type Run } from "@/components/CRunInfo.vue";
import { Temporal } from "temporal-polyfill";

export interface Task {
  repo: string;
  commit: {
    chash: string;
    title: string;
    author: { name: string };
    committer: { time: Temporal.Instant };
  };
  runs: Run[];
}

const { repo, commit, runs } = defineProps<Task>();
</script>

<template>
  <div class="flex gap-2">
    <div>-</div>
    <div class="flex flex-col">
      <div>
        <CLinkRepo :repo class="text-foreground-alt italic" />
        {{ " " }}
        <CLinkCommit
          :repo
          :chash="commit.chash"
          :title="commit.title"
          :author="commit.author.name"
          :time="commit.committer.time"
        />
      </div>

      <CRunInfo v-for="run in runs" :key="run.name" :repo :chash="commit.chash" :run small green />
    </div>
  </div>
</template>
