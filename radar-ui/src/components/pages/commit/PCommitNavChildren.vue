<script setup lang="ts">
import PCommitNavChildrenLink from "@/components/pages/commit/PCommitNavChildrenLink.vue";

const { repo, commits } = defineProps<{
  repo: string;
  commits: { chash: string; title: string; tracked: boolean }[];
}>();
</script>

<template>
  <template v-for="commit in commits" :key="commit.chash">
    <template v-if="commit.tracked">
      <div>Child:</div>
      <PCommitNavChildrenLink :repo v-bind="commit" />
    </template>
  </template>
</template>
