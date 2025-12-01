<script setup lang="ts">
import { useCommit } from "@/api/commit.ts";
import CCommitDetails from "@/components/CCommitDetails.vue";
import CLoading from "@/components/CLoading.vue";
import { reactive } from "vue";

const { repo, chash } = defineProps<{ repo: string; chash: string }>();

const commit = reactive(
  useCommit(
    () => repo,
    () => chash,
  ),
);
</script>

<template>
  <CLoading v-if="!commit.isSuccess" :error="commit.error" />
  <CCommitDetails v-else :repo :commit="commit.data.commit" />
</template>
