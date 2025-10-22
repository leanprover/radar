<script setup lang="ts">
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
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
    <CList>
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

  <CLoading v-if="!queue.isSuccess" :error="queue.error" />
  <PQueue v-else :tasks="queue.data.tasks" />
</template>
