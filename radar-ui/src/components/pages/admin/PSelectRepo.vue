<script setup lang="ts">
import { useRepos } from "@/api/repos.ts";
import { reactive } from "vue";

const repos = reactive(useRepos());
const repo = defineModel<string | undefined>();
</script>

<template>
  <label>
    Repo:
    <select v-model="repo" :disabled="!repos.isSuccess || repos.data.repos.length === 0" class="bg-background px-1">
      <option v-for="r in repos.data?.repos" :key="r.name" :value="r.name">{{ r.name }}</option>
    </select>
  </label>
</template>
