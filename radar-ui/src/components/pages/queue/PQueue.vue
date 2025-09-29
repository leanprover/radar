<script setup lang="ts">
import PQueueTask, { type Task } from "@/components/pages/queue/PQueueTask.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";

const { tasks } = defineProps<{ tasks: Task[] }>();
</script>

<template>
  <div class="flex flex-col">
    <CSectionTitle>Queue</CSectionTitle>
    <div v-if="tasks.length === 0" class="text-foreground-alt">empty \o/</div>
    <div v-else class="flex flex-col gap-2">
      <PQueueTask v-for="task in tasks" :key="JSON.stringify([task.repo, task.chash])" v-bind="task" />
    </div>
  </div>
</template>
