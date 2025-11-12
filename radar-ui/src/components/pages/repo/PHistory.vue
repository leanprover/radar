<script setup lang="ts">
import { type JsonEntry, useRepoHistory } from "@/api/repoHistory.ts";
import CControlFilter from "@/components/CControlFilter.vue";
import CControlPages from "@/components/CControlPages.vue";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CLoading from "@/components/CLoading.vue";
import CLinkCommit from "@/components/link/CLinkCommit.vue";
import { refDebounced } from "@vueuse/core";
import { computed, reactive, ref, watchEffect } from "vue";

const { repo } = defineProps<{ repo: string }>();

const page = ref(0);
const pageSize = ref(30);
const pageSizes = [30, 50, 100, 500, 1000];
const skip = computed(() => page.value * pageSize.value);

const filter = ref("");
const filterDebounced = refDebounced(filter, 300);
const filterOrUndefined = computed(() => filterDebounced.value || undefined);

const history = reactive(useRepoHistory(() => repo, { n: pageSize, skip, s: filterOrUndefined }));

const total = ref(0);
watchEffect(() => {
  if (!history.isSuccess) return;
  total.value = history.data.total;
});

function title(entry: JsonEntry): string | undefined {
  if (entry.enqueued) return "This commit is in the queue.";
  if (!entry.hasRuns) return "This commit hasn't been benchmarked yet.";
  if (entry.significant === true) return "This commit is significant.";
  return undefined;
}

function classes(entry: JsonEntry): string | undefined {
  if (entry.enqueued || !entry.hasRuns) return "text-foreground-alt italic";
  if (entry.significant === true) return "text-magenta font-bold";
}
</script>

<template>
  <div class="flex flex-col gap-2">
    <div class="flex max-w-[80ch] flex-col gap-1">
      <CControlFilter v-model="filter" label="Search:" placeholder="<chronological>" />
      <CControlPages v-model:page="page" v-model:page-size="pageSize" :page-sizes="pageSizes" :total />
    </div>

    <CLoading v-if="!history.isSuccess" :error="history.error" />
    <CList v-else>
      <div v-if="history.data.entries.length === 0">No commits found.</div>
      <CListItem v-for="entry in history.data.entries" :key="entry.commit.chash">
        <span :title="title(entry)">
          <CLinkCommit
            :repo
            :chash="entry.commit.chash"
            :title="entry.commit.title"
            :author="entry.commit.author.name"
            :time="entry.commit.committer.time"
            :class="classes(entry)"
          />
        </span>
      </CListItem>
    </CList>
  </div>
</template>
