<script setup lang="ts">
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useRepos } from "@/composables/useRepos.ts";

const { isPending, isSuccess, isError, data, error } = useRepos();
const selected = defineModel<string>();
</script>

<template>
  <Select v-model="selected" :disabled="!isSuccess">
    <SelectTrigger :title="error?.message" class="min-w-50">
      <SelectValue v-if="isPending">Loading repos...</SelectValue>
      <SelectValue v-else-if="isError" title="Hello" class="text-destructive">Error loading repos.</SelectValue>
      <SelectValue v-else-if="!selected" placeholder="Select a repo" />
      <SelectValue v-else>{{ selected }}</SelectValue>
    </SelectTrigger>
    <SelectContent v-if="isSuccess">
      <SelectItem :value="repo.name" :key="repo.name" v-for="repo of data">
        <div class="flex flex-col">
          <div>{{ repo.name }}</div>
          <div class="text-muted-foreground">{{ repo.description }}</div>
        </div>
      </SelectItem>
    </SelectContent>
  </Select>
</template>

<style scoped></style>
