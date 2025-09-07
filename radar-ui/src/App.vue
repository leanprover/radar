<script setup lang="ts">
import { useQuery } from "@tanstack/vue-query";
import * as api from "@/api.ts";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ref } from "vue";
import CColorMode from "@/components/CColorMode.vue";

const repos = useQuery({ queryKey: ["repos"], queryFn: api.getRepos });

const selectedRepo = ref<string>();
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <div class="flex border-b p-2">
      <Select v-model="selectedRepo" :disabled="!repos.isSuccess.value">
        <SelectTrigger :title="repos.error.value?.message">
          <SelectValue v-if="repos.isPending.value">Loading repos...</SelectValue>
          <SelectValue v-else-if="repos.isError.value" title="Hello" class="text-destructive">
            Error loading repos.
          </SelectValue>
          <SelectValue v-else-if="!selectedRepo" placeholder="Select a repo"></SelectValue>
          <SelectValue v-else>{{ selectedRepo }}</SelectValue>
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
      <div class="flex grow justify-end"><CColorMode></CColorMode></div>
    </div>
    <div
      v-if="!selectedRepo"
      class="text-muted-foreground bg-muted flex grow items-center justify-center text-xl font-light italic"
    >
      No repo selected
    </div>
  </div>
</template>
