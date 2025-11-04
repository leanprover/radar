<script setup lang="ts">
import { useRepoHistory } from "@/api/repoHistory.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlPages from "@/components/CControlPages.vue";
import CControlRow from "@/components/CControlRow.vue";
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

const query = ref("");
const queryDebounced = refDebounced(query, 300);
const queryOrUndefined = computed(() => queryDebounced.value || undefined);

const history = reactive(useRepoHistory(() => repo, { n: pageSize, skip, s: queryOrUndefined }));

const total = ref(0);
watchEffect(() => {
  if (!history.isSuccess) return;
  total.value = history.data.total;
});
</script>

<template>
  <div class="flex flex-col gap-2">
    <div class="flex max-w-[80ch] flex-col gap-1">
      <CControl>
        <CControlRow>
          <label for="query">Filter:</label>
          <input id="query" v-model="query" type="text" placeholder="<chronological>" class="bg-background grow px-1" />
          <CButton @click="query = ''">Clear</CButton>
        </CControlRow>
      </CControl>

      <CControlPages v-model:page="page" v-model:page-size="pageSize" :page-sizes="pageSizes" :total />
    </div>

    <CLoading v-if="!history.isSuccess" :error="history.error" />
    <CList v-else>
      <div v-if="history.data.entries.length === 0">No commits found.</div>
      <CListItem v-for="entry in history.data.entries" :key="entry.commit.chash">
        <span
          :title="
            !entry.hasRuns
              ? 'This commit hasn\'t been benchmarked yet.'
              : entry.significant === true
                ? 'This commit is significant.'
                : undefined
          "
        >
          <CLinkCommit
            :repo
            :chash="entry.commit.chash"
            :title="entry.commit.title"
            :author="entry.commit.author.name"
            :time="entry.commit.committer.time"
            :class="{
              'text-foreground-alt italic': !entry.hasRuns,
              'text-magenta font-bold': entry.hasRuns && entry.significant === true,
            }"
          />
        </span>
      </CListItem>
    </CList>
  </div>
</template>
