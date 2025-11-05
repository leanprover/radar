<script setup lang="ts">
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import PQueueTask, { type Task } from "@/components/pages/queue/PQueueTask.vue";

const { tasks } = defineProps<{ tasks: Task[] }>();
</script>

<template>
  <div v-if="tasks.length === 0" class="text-foreground-alt">empty \o/</div>
  <CList class="gap-2">
    <CListItem v-for="task in tasks" :key="JSON.stringify([task.repo, task.commit.chash])">
      <PQueueTask v-bind="task" />
    </CListItem>
  </CList>
</template>
