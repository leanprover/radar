<script setup lang="ts">
import { Temporal } from "temporal-polyfill";
import type { JsonRunResult } from "@/api/queue.ts";
import CTimeDurationSince from "@/components/CTimeDurationSince.vue";
import CTimeDurationBetween from "@/components/CTimeDurationBetween.vue";

export interface RunWithActiveRun {
  name: string;
  runner: string;
  active?: Temporal.Instant;
  result?: JsonRunResult;
}

const { name, runner, active = undefined, result = undefined } = defineProps<RunWithActiveRun>();
</script>

<template>
  <div v-if="active" class="text-blue text-xs">
    {{ name }} on {{ runner }}: running (<CTimeDurationSince :start="active" />)
  </div>
  <div v-else-if="result && result.exitCode === 0" class="text-green text-xs">
    {{ name }} on {{ runner }}: success (<CTimeDurationBetween :start="result.startTime" :end="result.endTime" />)
  </div>
  <div v-else-if="result" class="text-red text-xs">
    {{ name }} on {{ runner }}: error (<CTimeDurationBetween :start="result.startTime" :end="result.endTime" />)
  </div>
  <div v-else class="text-foreground-alt text-xs">{{ name }} on {{ runner }}: ready</div>
</template>
