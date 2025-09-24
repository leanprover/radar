<script setup lang="ts">
import { onBeforeRouteUpdate, useRoute } from "vue-router";
import { computed, reactive, ref } from "vue";
import { useCommitInfo } from "@/composables/useCommitInfo.ts";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import CLoading from "@/components/CLoading.vue";
import { CollapsibleContent, CollapsibleRoot, CollapsibleTrigger } from "reka-ui";
import { cn, formatDuration } from "@/lib/utils.ts";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { useRuns } from "@/composables/useRuns.ts";
import { useCompare } from "@/composables/useCompare.ts";
import CCommitMeasurementsTable from "@/components/CCommitMeasurementsTable.vue";

const route = useRoute("/repos.[repo].commits.[chash]");
const commit = reactive(
  useCommitInfo(
    () => route.params.repo,
    () => route.params.chash,
  ),
);
const runs = reactive(
  useRuns(
    () => route.params.repo,
    () => route.params.chash,
  ),
);
const compare = reactive(
  useCompare(
    () => route.params.repo,
    "parent",
    () => route.params.chash,
  ),
);
const measurements = computed(() => {
  if (!compare.isSuccess) return undefined;
  const data = compare.data.measurements.filter((it) => it.second !== undefined);
  if (data.length === 0) return undefined;
  return data;
});

const open = ref(false);

onBeforeRouteUpdate(() => {
  open.value = false;
});
</script>

<template>
  <CLoading v-if="!commit.isSuccess" :error="commit.error" />
  <div v-else class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
    <CSectionTitle>Commit</CSectionTitle>

    <!-- TODO Link to GitHub -->
    <div class="text-yellow col-span-2">commit {{ commit.data.chash }}</div>

    <div>Author:</div>
    <div>{{ commit.data.author.name }} &lt;{{ commit.data.author.email }}&gt;</div>

    <div>Date:</div>
    <div>
      {{ useDateFormat(commit.data.author.time.epochMilliseconds, "YYYY-MM-DD HH:mm:ss") }}
      ({{ useTimeAgo(commit.data.author.time.epochMilliseconds) }})
    </div>

    <CollapsibleRoot
      v-model:open="open"
      :disabled="!commit.data.body"
      class="col-span-2 my-3 ml-[4ch] flex flex-col gap-3"
    >
      <CollapsibleTrigger :class="cn('text-left', { 'cursor-pointer': commit.data.body })">
        <span>{{ commit.data.body ? (open ? "v" : "^") : "-" }}{{ " " }}</span>
        <span class="font-bold">{{ commit.data.title }}</span>
      </CollapsibleTrigger>
      <CollapsibleContent class="max-w-[80ch] whitespace-pre-wrap">{{ commit.data.body }}</CollapsibleContent>
    </CollapsibleRoot>

    <template v-for="parent in commit.data.parents" :key="parent.chash">
      <div>Parent:</div>
      <RouterLink
        :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: parent.chash } }"
        :title="parent.title"
        :class="cn('cursor-pointer truncate italic hover:underline', { 'text-foreground-alt': !parent.tracked })"
      >
        &lt; {{ parent.title }}
      </RouterLink>
    </template>
    <template v-for="child in commit.data.children" :key="child.chash">
      <div>Child:</div>
      <RouterLink
        :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: child.chash } }"
        :title="child.title"
        :class="cn('cursor-pointer truncate italic hover:underline', { 'text-foreground-alt': !child.tracked })"
      >
        &gt; {{ child.title }}
      </RouterLink>
    </template>
  </div>

  <CLoading v-if="!runs.isSuccess" :error="runs.error" />
  <div v-else-if="runs.data.runs.length > 0" class="flex flex-col">
    <CSectionTitle>Runs</CSectionTitle>
    <div v-for="run in runs.data.runs" :key="run.name" class="flex gap-2">
      <div>-</div>
      <div>
        {{ run.name }} ({{ run.script }} on {{ run.runner }}) in {{ formatDuration(run.startTime.until(run.endTime)) }}
      </div>
    </div>
  </div>

  <CLoading v-if="!compare.isSuccess" :error="compare.error" />
  <div v-else-if="measurements !== undefined" class="flex flex-col">
    <CSectionTitle>Measurements</CSectionTitle>
    <CCommitMeasurementsTable :measurements="measurements" />
  </div>
</template>
