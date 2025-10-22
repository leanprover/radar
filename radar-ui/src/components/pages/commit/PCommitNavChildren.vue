<script setup lang="ts">
import PCommitNavChildrenLink from "@/components/pages/commit/PCommitNavChildrenLink.vue";

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
    <template v-if="commit.tracked">
      <div>Child:</div>
      <PCommitNavChildrenLink :repo :search v-bind="commit" />
    </template>
  </template>
</template>
