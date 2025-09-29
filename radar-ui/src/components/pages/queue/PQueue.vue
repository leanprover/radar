<script setup lang="ts">
import PQueueTask, { type Task } from "@/components/pages/queue/PQueueTask.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CSection from "@/components/CSection.vue";

const { tasks } = defineProps<{ tasks: Task[] }>();
</script>

<template>
  <CSection>
    <CSectionTitle>Queue</CSectionTitle>
    <div v-if="tasks.length === 0" class="text-foreground-alt">empty \o/</div>
    <div v-else class="flex flex-col gap-2">
      <PQueueTask v-for="task in tasks" :key="JSON.stringify([task.repo, task.chash])" v-bind="task" />
    </div>
  </CSection>
</template>
