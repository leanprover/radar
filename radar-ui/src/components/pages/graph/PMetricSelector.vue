<script setup lang="ts">
import { useRepoMetrics } from "@/api/repoMetrics.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlRow from "@/components/CControlRow.vue";
import CPlural from "@/components/format/CPlural.vue";
import { metricFilterMatches, parseMetric } from "@/lib/utils.ts";
import { computed, reactive, ref, watchEffect } from "vue";

const { repo, limit } = defineProps<{ repo: string; limit: number }>();
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
  else if (selected.value.size >= limit) return;
  else result.add(metric);
  selected.value = result;
}

const pageSize = ref(100);
const pageSizes = [100, 500, 1000, 5000];
const page = ref(0);
const pages = computed(() => Math.ceil(visibleMetrics.value.length / pageSize.value));

watchEffect(() => {
  if (page.value >= pages.value) page.value = pages.value - 1;
  if (page.value < 0) page.value = 0;
});

const pageMetrics = computed(() => {
  const start = pageSize.value * page.value;
  const end = start + pageSize.value;
  return visibleMetrics.value.slice(start, end);
});
</script>

<template>
  <div class="flex flex-col gap-1">
    <CControl>
      <CControlRow>
        <div class="shrink-0">Filter:</div>
        <input v-model="filter" type="text" placeholder="Enter a regex..." class="bg-background shrink-0 grow px-1" />
        <CButton
          :disabled="visibleMetrics.length > limit"
          class="shrink-0"
          :title="visibleMetrics.length > limit ? `Too many metrics, can select at most ${limit}` : undefined"
          @click="selected = new Set(visibleMetrics)"
        >
          Select {{ visibleMetrics.length }} <CPlural :n="visibleMetrics.length">metric</CPlural>
        </CButton>
        <CButton class="shrink-0" @click="selected = new Set()">Unselect all</CButton>
      </CControlRow>
    </CControl>

    <CControl>
      <CControlRow>
        <CButton @click="page = 0">First</CButton>
        <CButton @click="page -= 1">Prev</CButton>
        <div>Page {{ page + 1 }} / {{ pages }}</div>
        <CButton @click="page += 1">Next</CButton>
        <CButton @click="page = pages - 1">Last</CButton>
        <label class="ml-auto">
          Rows:
          <select v-model="pageSize" class="bg-background px-1">
            <option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option>
          </select>
        </label>
      </CControlRow>
    </CControl>

    <div class="grid grid-cols-[auto_auto_auto_1fr] overflow-y-scroll">
      <div v-for="metric in pageMetrics" :key="metric" class="group contents cursor-default" @click="toggle(metric)">
        <div class="group-hover:bg-background-alt pr-2 pl-1">
          <input
            type="checkbox"
            :checked="selected.has(metric)"
            :disabled="!selected.has(metric) && selected.size >= limit"
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
