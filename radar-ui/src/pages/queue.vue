<script setup lang="ts">
import { reactive } from "vue";
import { useQueue } from "@/composables/useQueue.ts";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CLoading from "@/components/CLoading.vue";
import PQueue from "@/components/pages/queue/PQueue.vue";
import CTimeAgo from "@/components/CTimeAgo.vue";

const queue = reactive(useQueue());
</script>

<template>
  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <div v-else class="flex flex-col">
    <CSectionTitle>Runners</CSectionTitle>
    <div v-for="runner in queue.data.runners" :key="runner.name" class="flex items-baseline gap-2">
      <div>-</div>
      <div>{{ runner.name }}</div>
      <div class="text-foreground-alt text-xs">
        <template v-if="runner.connected">(connected)</template>
        <template v-else-if="runner.lastSeen">
          (last seen <CTimeAgo :when="runner.lastSeen" class="hover:text-foreground" />)
        </template>
        <template v-else>(never seen)</template>
      </div>
    </div>
  </div>

  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <PQueue v-else :tasks="queue.data.tasks" />
</template>
