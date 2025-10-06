<script setup lang="ts">
import PCommitNavParentsLink from "@/components/pages/commit/PCommitNavParentsLink.vue";

const { repo, commits } = defineProps<{
  repo: string;
  commits: { chash: string; title: string; tracked: boolean }[];
}>();
</script>

<template>
  <template v-for="commit in commits" :key="commit.chash">
    <div>Parent:</div>
    <PCommitNavParentsLink :repo v-bind="commit" />
  </template>
</template>
