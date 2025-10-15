<script setup lang="ts">
import CLinkCommitHash from "@/components/CLinkCommitHash.vue";
import CTimeAgo from "@/components/CTimeAgo.vue";
import CTimeInstant from "@/components/CTimeInstant.vue";
import PCommitMessage from "@/components/pages/commit/PCommitMessage.vue";
import { Temporal } from "temporal-polyfill";

const { repo, url, chash, author, title, body } = defineProps<{
  repo: string;
  url: string | undefined;
  chash: string;
  author: { name: string; email: string; time: Temporal.Instant };
  title: string;
  body: string | undefined;
}>();
</script>

<template>
  <div class="grid grid-cols-[auto_1fr] gap-x-2">
    <CLinkCommitHash :repo="repo" :url="url" :chash="chash" class="text-yellow col-span-full" />

    <div>Author:</div>
    <div>{{ author.name }} &lt;{{ author.email }}&gt;</div>

    <div>Date:</div>
    <div>
      <CTimeInstant :when="author.time" />
      <span class="text-foreground-alt text-xs"> (<CTimeAgo :when="author.time" />)</span>
    </div>

    <PCommitMessage :title="title" :body="body" />
  </div>
</template>
