<script setup lang="ts">
import CLoading from "@/components/CLoading.vue";
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CTimeAgo from "@/components/CTimeAgo.vue";
import PQueue from "@/components/pages/queue/PQueue.vue";
import { useQueue } from "@/composables/useQueue.ts";
import { reactive } from "vue";

const queue = reactive(useQueue());
</script>

<template>
  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <CSection v-else>
    <CSectionTitle>Runners</CSectionTitle>
    <div class="flex flex-col">
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
  </CSection>

  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <PQueue v-else :tasks="queue.data.tasks" />
</template>
