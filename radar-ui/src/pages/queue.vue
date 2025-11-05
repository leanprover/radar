<script setup lang="ts">
import { useQueue } from "@/api/queue.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CLoading from "@/components/CLoading.vue";
import CSection from "@/components/CSection.vue";
import CTimeAgo from "@/components/format/CTimeAgo.vue";
import PQueue from "@/components/pages/queue/PQueue.vue";
import { reactive } from "vue";

const queue = reactive(useQueue());
</script>

<template>
  <CSection title="Runners">
    <CLoading v-if="!queue.isSuccess" :error="queue.error" />
    <CList v-else>
      <div v-if="queue.data.runners.length === 0">No runners.</div>
      <CListItem v-for="runner in queue.data.runners" :key="runner.name" class="items-baseline">
        <div>{{ runner.name }}</div>
        <div class="text-foreground-alt text-xs">
          <template v-if="runner.connected">(connected)</template>
          <template v-else-if="runner.lastSeen">
            (last seen <CTimeAgo :when="runner.lastSeen" class="hover:text-foreground" />)
          </template>
          <template v-else>(never seen)</template>
        </div>
      </CListItem>
    </CList>
  </CSection>

  <CSection title="Queue">
    <CLoading v-if="!queue.isSuccess" :error="queue.error" />
    <PQueue v-else :tasks="queue.data.tasks" />
  </CSection>
</template>
