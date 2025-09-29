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

const { name, runner, active = undefined, finished = undefined } = defineProps<Run>();
</script>

<template>
  <div v-if="finished && finished.exitCode === 0" class="text-green text-xs">
    {{ name }} on {{ runner }}: success (<CTimeDurationBetween :start="finished.startTime" :end="finished.endTime" />)
  </div>
  <div v-else-if="finished" class="text-red text-xs">
    {{ name }} on {{ runner }}: error (<CTimeDurationBetween :start="finished.startTime" :end="finished.endTime" />)
  </div>
  <div v-else-if="active" class="text-blue text-xs">
    {{ name }} on {{ runner }}: running (<CTimeDurationSince :start="active.startTime" />)
  </div>
  <div v-else class="text-foreground-alt text-xs">{{ name }} on {{ runner }}: ready</div>
</template>
