<script setup lang="ts">
import CTimeDurationBetween from "@/components/format/CTimeDurationBetween.vue";
import CTimeDurationSince from "@/components/format/CTimeDurationSince.vue";
import CLink from "@/components/link/CLink.vue";
import { Temporal } from "temporal-polyfill";

export interface Run {
  name: string;
  runner: string;
  active?: { startTime: Temporal.Instant };
  finished?: { startTime: Temporal.Instant; endTime: Temporal.Instant; exitCode: number };
}

const {
  repo,
  chash,
  run,
  small = false,
  green = false,
} = defineProps<{
  repo: string;
  chash: string;
  run: Run;
  small?: boolean;
  green?: boolean;
}>();
</script>

<template>
  <CLink v-if="run.finished && run.finished.exitCode === 0" :class="{ 'text-xs': small, 'text-green': green }">
    <RouterLink :to="{ name: '/repos.[repo].commits.[chash].runs.[run]', params: { repo, chash, run: run.name } }">
      {{ run.name }} on {{ run.runner }}: success (<CTimeDurationBetween
        :start="run.finished.startTime"
        :end="run.finished.endTime"
      />)
    </RouterLink>
  </CLink>
  <CLink v-else-if="run.finished" :class="{ 'text-red': true, 'text-xs': small }">
    <RouterLink :to="{ name: '/repos.[repo].commits.[chash].runs.[run]', params: { repo, chash, run: run.name } }">
      {{ run.name }} on {{ run.runner }}: error (<CTimeDurationBetween
        :start="run.finished.startTime"
        :end="run.finished.endTime"
      />)
    </RouterLink>
  </CLink>
  <CLink v-else-if="run.active" :class="{ 'text-blue': true, 'text-xs': small }">
    <RouterLink :to="{ name: '/queue.runs.[repo].[chash].[run]', params: { repo, chash, run: run.name } }">
      {{ run.name }} on {{ run.runner }}: running (<CTimeDurationSince :start="run.active.startTime" />)
    </RouterLink>
  </CLink>
  <CLink v-else :class="{ 'text-foreground-alt': true, 'text-xs': small }">
    <RouterLink :to="{ name: '/queue.runs.[repo].[chash].[run]', params: { repo, chash, run: run.name } }">
      {{ run.name }} on {{ run.runner }}: awaiting runner
    </RouterLink>
  </CLink>
</template>
