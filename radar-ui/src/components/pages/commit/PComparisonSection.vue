<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import PMessage from "@/components/pages/commit/PMessage.vue";

const {
  title,
  messages,
  open = false,
} = defineProps<{
  title: string;
  messages: JsonMessageSegment[][];
  open?: boolean;
}>();
</script>

<template>
  <details v-if="messages.length > 0" :open>
    <summary>{{ title }} ({{ messages.length }})</summary>
    <CList class="mt-2">
      <CListItem v-for="(msg, i) in messages" :key="i">
        <PMessage :segments="msg" />
      </CListItem>
    </CList>
  </details>
</template>
