<script setup lang="ts">
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useQuery } from "@tanstack/vue-query";
import * as api from "@/api.ts";

const repos = useQuery({ queryKey: ["repos"], queryFn: api.getRepos });
const selected = defineModel<string>();
</script>

<template>
  <Select v-model="selected" :disabled="!repos.isSuccess.value">
    <SelectTrigger :title="repos.error.value?.message" class="min-w-50">
      <SelectValue v-if="repos.isPending.value">Loading repos...</SelectValue>
      <SelectValue v-else-if="repos.isError.value" title="Hello" class="text-destructive">
        Error loading repos.
      </SelectValue>
      <SelectValue v-else-if="!selected" placeholder="Select a repo"></SelectValue>
      <SelectValue v-else>{{ selected }}</SelectValue>
    </SelectTrigger>
    <SelectContent v-if="repos.isSuccess.value">
      <SelectItem :value="repo.name" :key="repo.name" v-for="repo of repos.data.value">
        <div class="flex flex-col">
          <div>{{ repo.name }}</div>
          <div class="text-muted-foreground">{{ repo.description }}</div>
        </div>
      </SelectItem>
    </SelectContent>
  </Select>
</template>

<style scoped></style>
