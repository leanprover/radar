<script setup lang="ts">
import { invalidateAdminRepoMetrics, type JsonMetric, useAdminRepoMetrics } from "@/api/adminRepoMetrics.ts";
import { postAdminRepoMetricsRename } from "@/api/adminRepoMetricsRename.ts";
import CButton from "@/components/CButton.vue";
import CControlFilter from "@/components/CControlFilter.vue";
import CControlPages from "@/components/CControlPages.vue";
import CControlRow from "@/components/CControlRow.vue";
import CPlural from "@/components/format/CPlural.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { useQueryClient } from "@tanstack/vue-query";
import { computed, reactive, ref } from "vue";

const { repo } = defineProps<{ repo: string }>();
const queryClient = useQueryClient();

const pattern = ref("");
const replace = ref("");

const metricsQuery = reactive(useAdminRepoMetrics(() => repo));

const allMetrics = computed<JsonMetric[]>(() => {
  if (!metricsQuery.data) return [];
  return metricsQuery.data.metrics;
});

const visibleMetrics = computed(() => {
  const regex = new RegExp(pattern.value);

  const result: { metric: JsonMetric; replacement: string }[] = [];
  for (const metric of allMetrics.value) {
    const match = regex.exec(metric.metric);
    if (match === null) continue;
    const replacement = replace.value.replace(/\{\{|\{(\d+)}/g, (_, n: string | undefined) => {
      if (n === undefined) return "{";
      const group = parseInt(n, 10);
      return match[group] ?? "<unmatched>";
    });
    result.push({ metric, replacement });
  }
  return result;
});

const page = ref(0);
const pageSize = ref(30);
const pageSizes = [30, 50, 100, 500, 1000, 5000];

const pageMetrics = computed(() => {
  const start = pageSize.value * page.value;
  const end = start + pageSize.value;
  return visibleMetrics.value.slice(start, end);
});

const allReplacementsUnique = computed(() => {
  const replacements = new Set(visibleMetrics.value.map((it) => it.replacement));
  return replacements.size === visibleMetrics.value.length;
});

const admin = useAdminStore();
async function onClick() {
  if (admin.token === undefined) return;

  const metrics = new Map<string, string>();
  for (const metric of visibleMetrics.value) {
    metrics.set(metric.metric.metric, metric.replacement);
  }

  await postAdminRepoMetricsRename(admin.token, repo, metrics);
  await invalidateAdminRepoMetrics(queryClient, repo);
}
</script>

<template>
  <div class="flex flex-col gap-1">
    <CControlFilter v-model="pattern" label="Pattern:" />
    <CControlFilter v-model="replace" label="Replace:" />

    <CControlPages
      v-model:page="page"
      v-model:page-size="pageSize"
      :page-sizes="pageSizes"
      :total="visibleMetrics.length"
    />

    <div class="grid grid-cols-[auto_auto_auto_1fr] gap-x-2 overflow-y-scroll">
      <div v-if="allMetrics.length === 0">No metrics.</div>
      <div v-else-if="visibleMetrics.length === 0">No metrics match the filter.</div>
      <div v-for="metric in pageMetrics" :key="metric.metric.metric" class="group contents cursor-default">
        <div>{{ metric.metric.metric }}</div>
        <div :class="{ 'font-bold': metric.metric.appearsInLatestCommit }">
          ({{ metric.metric.appearsInHistoricalCommits }})
        </div>
        <div>→</div>
        <div v-if="metric.replacement">{{ metric.replacement }}</div>
        <div v-else class="text-foreground-alt italic">empty string</div>
      </div>
    </div>

    <CControlRow>
      <CButton
        :disabled="!allReplacementsUnique"
        :title="allReplacementsUnique ? '' : 'Not all replacements are unique'"
        @click="onClick()"
      >
        Rename {{ visibleMetrics.length }} <CPlural :n="visibleMetrics.length">metric</CPlural>
      </CButton>
      <CButton disabled title="Not implemented yet">
        Delete {{ visibleMetrics.length }} <CPlural :n="visibleMetrics.length">metric</CPlural>
      </CButton>
    </CControlRow>
  </div>
</template>
