<script setup lang="ts">
import PCommitNavParentsLink from "@/components/pages/commit/PCommitNavParentsLink.vue";

const {
  repo,
  search = undefined,
  commits,
} = defineProps<{
  repo: string;
  search?: string;
  commits: { chash: string; title: string; tracked: boolean }[];
}>();
</script>

<template>
  <template v-for="commit in commits" :key="commit.chash">
    <div>Parent:</div>
    <PCommitNavParentsLink :repo :search v-bind="commit" />
  </template>
</template>
