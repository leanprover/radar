<script setup lang="ts">
import { type JsonCommit } from "@/api/types.ts";
import CTimeAgo from "@/components/format/CTimeAgo.vue";
import CTimeInstant from "@/components/format/CTimeInstant.vue";
import CLinkCommitHash from "@/components/link/CLinkCommitHash.vue";
import PCommitMessage from "@/components/pages/commit/PCommitMessage.vue";

const {
  repo = undefined,
  repoUrl = undefined,
  commit,
} = defineProps<{
  repo?: string;
  repoUrl?: string;
  commit: JsonCommit;
}>();
</script>

<template>
  <div class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
    <div class="text-yellow col-span-full">commit <CLinkCommitHash :repo :url="repoUrl" :chash="commit.chash" /></div>

    <div>Author:</div>
    <div>{{ commit.author.name }} &lt;{{ commit.author.email }}&gt;</div>

    <div>Date:</div>
    <div>
      <CTimeInstant :when="commit.author.time" />
      <span class="text-foreground-alt text-xs"> (<CTimeAgo :when="commit.author.time" />)</span>
    </div>

    <PCommitMessage :title="commit.title" :body="commit.body" class="mt-3" />
  </div>
</template>
