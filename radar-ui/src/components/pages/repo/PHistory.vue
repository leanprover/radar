<script setup lang="ts">
import { useRepoHistory } from "@/api/repoHistory.ts";
import CControlPages from "@/components/CControlPages.vue";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CLoading from "@/components/CLoading.vue";
import CLinkCommit from "@/components/link/CLinkCommit.vue";
import { computed, reactive, ref, watchEffect } from "vue";

const { repo } = defineProps<{ repo: string }>();

const page = ref(0);
const pageSize = ref(30);
const pageSizes = [30, 50, 100, 500, 1000];
const skip = computed(() => page.value * pageSize.value);

const history = reactive(useRepoHistory(() => repo, { n: pageSize, skip }));

const total = ref(0);
watchEffect(() => {
  if (!history.isSuccess) return;
  total.value = history.data.total;
});
</script>

<template>
  <div class="flex flex-col gap-2">
    <CControlPages
      v-model:page="page"
      v-model:page-size="pageSize"
      :page-sizes="pageSizes"
      :total
      class="max-w-[80ch]"
    />

    <CLoading v-if="!history.isSuccess" :error="history.error" />
    <CList v-else>
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
