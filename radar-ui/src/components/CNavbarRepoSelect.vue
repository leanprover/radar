<script setup lang="ts">
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useRepos } from "@/composables/useRepos.ts";
import { reactive, watchEffect } from "vue";

const repos = reactive(useRepos());
const selected = defineModel<string>();

// No repo should ever be selected that doesn't actually exist.
watchEffect(() => {
  // We don't want to erase the selection before we know which repos exist.
  if (!repos.isSuccess) return;

  const selectedValue = selected.value;
  if (repos.data.repos.some((it) => it.name === selectedValue)) return;
  selected.value = undefined;
});
</script>

<template>
  <Select v-model="selected" :disabled="!repos.isSuccess">
    <SelectTrigger :title="repos.error?.message" class="min-w-50">
      <SelectValue v-if="repos.isPending">Loading repos...</SelectValue>
      <SelectValue v-else-if="repos.isError" title="Hello" class="text-destructive">Error loading repos.</SelectValue>
      <SelectValue v-else-if="!selected" placeholder="Select a repo" />
      <SelectValue v-else>{{ selected }}</SelectValue>
    </SelectTrigger>
    <SelectContent v-if="repos.isSuccess">
      <SelectItem :value="repo.name" :key="repo.name" v-for="repo of repos.data.repos">
        <div class="flex flex-col">
          <div>{{ repo.name }}</div>
          <div class="text-muted-foreground">{{ repo.description }}</div>
        </div>
      </SelectItem>
    </SelectContent>
  </Select>
</template>
