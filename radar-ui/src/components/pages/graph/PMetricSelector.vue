<script setup lang="ts">
import { metricsLimit } from "@/api/repoGraph.ts";
import { useRepoMetrics } from "@/api/repoMetrics.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlFilter from "@/components/CControlFilter.vue";
import CControlPages from "@/components/CControlPages.vue";
import CControlRow from "@/components/CControlRow.vue";
import CPlural from "@/components/format/CPlural.vue";
import { metricFilterMatches, parseMetric } from "@/lib/utils.ts";
import { computed, reactive, ref } from "vue";

const { repo } = defineProps<{ repo: string }>();
const filter = defineModel<string>("filter", { required: true });
const selected = defineModel<Set<string>>("selected", { required: true });

const metricsQuery = reactive(useRepoMetrics(() => repo));

const allMetrics = computed(() => {
  const result = new Set<string>();
  for (const metric of selected.value) result.add(metric);
  if (metricsQuery.data) for (const metric of metricsQuery.data.metrics) result.add(metric.metric);
  return Array.from(result).sort();
});

const visibleMetrics = computed(() => {
  if (!filter.value) return allMetrics.value;
  return allMetrics.value.filter((it) => metricFilterMatches(filter.value, it));
});

function toggle(metric: string) {
  // Not deeply reactive, so we always create a new Set
  const result = new Set(selected.value);
  if (result.has(metric)) result.delete(metric);
  else if (selected.value.size >= metricsLimit) return;
  else result.add(metric);
  selected.value = result;
}

const page = ref(0);
const pageSize = ref(100);
const pageSizes = [100, 500, 1000, 5000];

const pageMetrics = computed(() => {
  const start = pageSize.value * page.value;
  const end = start + pageSize.value;
  return visibleMetrics.value.slice(start, end);
});
</script>

<template>
  <div class="flex flex-col gap-1">
    <CControlFilter v-model="filter" placeholder="Enter a regex..." />

    <CControl>
      <CControlRow>
        <div v-if="filter" class="grow">
          {{ visibleMetrics.length }} filtered <CPlural :n="visibleMetrics.length">metric</CPlural>,
          {{ selected.size }} selected.
        </div>
        <div v-else class="grow">
          {{ allMetrics.length }} <CPlural :n="allMetrics.length">metric</CPlural>, {{ selected.size }} selected.
        </div>
        <CButton
          :disabled="visibleMetrics.length > metricsLimit"
          class="shrink-0"
          :title="
            visibleMetrics.length > metricsLimit ? `Too many metrics, can select at most ${metricsLimit}` : undefined
          "
          @click="selected = new Set(visibleMetrics)"
        >
          Select visible
        </CButton>
        <CButton class="shrink-0" @click="selected = new Set()">Reset</CButton>
      </CControlRow>
    </CControl>

    <CControlPages
      v-model:page="page"
      v-model:page-size="pageSize"
      :page-sizes="pageSizes"
      :total="visibleMetrics.length"
    />

    <div class="grid grid-cols-[auto_auto_auto_1fr] overflow-y-scroll">
      <div v-if="allMetrics.length === 0">No metrics.</div>
      <div v-else-if="visibleMetrics.length === 0">No metrics match the filter.</div>
      <div v-for="metric in pageMetrics" :key="metric" class="group contents cursor-default" @click="toggle(metric)">
        <div class="group-hover:bg-background-alt pr-2 pl-1">
          <input
            type="checkbox"
            :checked="selected.has(metric)"
            :disabled="!selected.has(metric) && selected.size >= metricsLimit"
            class="align-[-2px]"
            @change="toggle(metric)"
            @click.stop
          />
        </div>
        <div class="group-hover:bg-background-alt">{{ parseMetric(metric)[0] }}</div>
        <div class="group-hover:bg-background-alt px-2">{{ parseMetric(metric)[1] && "//" }}</div>
        <div class="group-hover:bg-background-alt pr-1">{{ parseMetric(metric)[1] }}</div>
      </div>
    </div>
  </div>
</template>
