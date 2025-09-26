<script setup lang="ts">
import { onBeforeRouteUpdate, useRoute } from "vue-router";
import { computed, reactive, ref } from "vue";
import { useCommit } from "@/composables/useCommit.ts";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import CLoading from "@/components/CLoading.vue";
import { CollapsibleContent, CollapsibleRoot, CollapsibleTrigger } from "reka-ui";
import { cn, formatDuration } from "@/lib/utils.ts";
import CSectionTitle from "@/components/CSectionTitle.vue";
import { useCompare } from "@/composables/useCompare.ts";
import CCommitMeasurementsTable from "@/components/CCommitMeasurementsTable.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { postAdminEnqueue } from "@/api/adminEnqueue.ts";

const route = useRoute("/repos.[repo].commits.[chash]");
const admin = useAdminStore();

const commit = reactive(
  useCommit(
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

    <CollapsibleRoot v-model:open="open" :disabled="!commit.data.body" class="col-span-2 my-3 ml-[4ch] flex flex-col">
      <CollapsibleTrigger
        :class="
          cn(
            'text-left',

            { 'cursor-pointer': commit.data.body },
          )
        "
      >
        <span>{{ commit.data.body ? (open ? "v" : "^") : "-" }}{{ " " }}</span>
        <span class="font-bold">{{ commit.data.title }}</span>
      </CollapsibleTrigger>
      <CollapsibleContent
        :class="[
          'overflow-hidden',
          'data-[state=closed]:animate-[slideUp_200ms_ease-out]',
          'data-[state=open]:animate-[slideDown_200ms_ease-out]',
        ]"
      >
        <pre class="max-w-[80ch] pt-3 whitespace-pre-wrap">{{ commit.data.body }}</pre>
      </CollapsibleContent>
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

  <div v-if="admin.token !== undefined" class="col-span-2 flex flex-col">
    <CSectionTitle>Admin</CSectionTitle>
    <div class="flex gap-2">
      <button
        class="bg-foreground text-background hover:bg-foreground-alt cursor-pointer px-1"
        @click="postAdminEnqueue(admin.token, route.params.repo, route.params.chash)"
      >
        Enqueue
      </button>
    </div>
  </div>

  <div v-if="commit.isSuccess && commit.data.runs.length > 0" class="flex flex-col">
    <CSectionTitle>Runs</CSectionTitle>
    <div v-for="run in commit.data.runs" :key="run.name" class="flex gap-2">
      <div>-</div>
      <RouterLink
        :to="{
          name: '/repos.[repo].commits.[chash].runs.[run]',
          params: { repo: route.params.repo, chash: route.params.chash, run: run.name },
        }"
        class="hover:underline"
      >
        {{ run.name }} ({{ run.script }} on {{ run.runner }}) in {{ formatDuration(run.startTime.until(run.endTime)) }}
      </RouterLink>
    </div>
  </div>

  <CLoading v-if="!compare.isSuccess" :error="compare.error" />
  <div v-else-if="measurements !== undefined" class="flex flex-col">
    <CSectionTitle>Measurements</CSectionTitle>
    <CCommitMeasurementsTable :measurements="measurements" />
  </div>
</template>

<style>
@keyframes slideDown {
  from {
    height: 0;
  }
  to {
    height: var(--reka-collapsible-content-height);
  }
}

@keyframes slideUp {
  from {
    height: var(--reka-collapsible-content-height);
  }
  to {
    height: 0;
  }
}
</style>
