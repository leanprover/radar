<script setup lang="ts">
import type { JsonMessageSegment } from "@/api/types.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import PDetailsMessage from "@/components/pages/commit/PDetailsMessage.vue";

const { repo, chash, title, messages } = defineProps<{
  repo: string;
  chash: string;
  title: string;
  messages: JsonMessageSegment[][];
}>();
</script>

<template>
  <template v-if="messages.length > 0">
    <div>{{ title }} ({{ messages.length }})</div>
    <CList>
      <CListItem v-for="(msg, i) in messages" :key="i">
        <PDetailsMessage :repo :chash :segments="msg" />
      </CListItem>
    </CList>
  </template>
</template>
