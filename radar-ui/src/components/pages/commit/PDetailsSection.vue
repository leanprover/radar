<script setup lang="ts">
import type { JsonMessage } from "@/api/types.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import PDetailsMessage from "@/components/pages/commit/PDetailsMessage.vue";

const { repo, chash, reference, title, messages } = defineProps<{
  repo: string;
  chash: string;
  reference: string;
  title: string;
  messages: JsonMessage[];
}>();

const markers = { GOOD: "+", BAD: "-", NEUTRAL: "~" };
const colors = { GOOD: "text-green", BAD: "text-red", NEUTRAL: undefined };
</script>

<template>
  <template v-if="messages.length > 0">
    <div>{{ title }} ({{ messages.length }})</div>
    <CList>
      <CListItem
        v-for="(message, i) in messages"
        :key="i"
        :marker="markers[message.goodness]"
        :class="colors[message.goodness]"
      >
        <PDetailsMessage :repo :chash :reference :message />
      </CListItem>
    </CList>
  </template>
</template>
