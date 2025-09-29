<script setup lang="ts">
import CTimeDurationSince from "@/components/CTimeDurationSince.vue";
import CTimeDurationBetween from "@/components/CTimeDurationBetween.vue";
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
  <RouterLink
    v-if="run.finished && run.finished.exitCode === 0"
    :to="{ name: '/repos.[repo].commits.[chash].runs.[run]', params: { repo, chash, run: run.name } }"
    :class="{ 'hover:underline': true, 'text-xs': small, 'text-green': green }"
  >
    {{ run.name }} on {{ run.runner }}: success (<CTimeDurationBetween
      :start="run.finished.startTime"
      :end="run.finished.endTime"
    />)
  </RouterLink>
  <RouterLink
    v-else-if="run.finished"
    :to="{ name: '/repos.[repo].commits.[chash].runs.[run]', params: { repo, chash, run: run.name } }"
    :class="{ 'text-red hover:underline': true, 'text-xs': small }"
  >
    {{ run.name }} on {{ run.runner }}: error (<CTimeDurationBetween
      :start="run.finished.startTime"
      :end="run.finished.endTime"
    />)
  </RouterLink>
  <div v-else-if="run.active" :class="{ 'text-blue': true, 'text-xs': small }">
    {{ run.name }} on {{ run.runner }}: running (<CTimeDurationSince :start="run.active.startTime" />)
  </div>
  <div v-else :class="{ 'text-foreground-alt': true, 'text-xs': small }">{{ run.name }} on {{ run.runner }}: ready</div>
</template>
