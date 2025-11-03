<script setup lang="ts">
import { useRepos } from "@/api/repos.ts";
import { SelectContent, SelectIcon, SelectItem, SelectPortal, SelectRoot, SelectTrigger, SelectValue } from "reka-ui";
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
  <SelectRoot v-model="selected">
    <SelectTrigger
      class="hover:bg-background-alt flex min-w-[20ch] cursor-pointer justify-between gap-2 px-1"
      title="Switch between repos"
    >
      <SelectValue v-if="repos.isPending" class="text-foreground-alt">Loading repos...</SelectValue>
      <SelectValue v-else-if="repos.isError" class="text-red font-bold">Error loading repos</SelectValue>
      <SelectValue v-else-if="!selected" placeholder="Select a repo" class="text-foreground-alt" />
      <SelectValue v-else>{{ selected }}</SelectValue>
      <SelectIcon>v</SelectIcon>
    </SelectTrigger>
    <SelectPortal v-if="repos.isSuccess">
      <SelectContent position="popper" class="bg-background z-[100] min-w-[20ch] border">
        <SelectItem
          v-for="repo in repos.data.repos"
          :key="repo.name"
          :value="repo.name"
          class="hover:bg-background-alt cursor-default px-1"
        >
          <div class="flex flex-col">
            <div>{{ repo.name }}</div>
            <div class="text-foreground-alt text-xs">{{ repo.description }}</div>
          </div>
        </SelectItem>
      </SelectContent>
    </SelectPortal>
  </SelectRoot>
</template>
