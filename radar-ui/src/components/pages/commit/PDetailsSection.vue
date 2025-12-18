<script setup lang="ts">
import type { JsonSignificance } from "@/api/types.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import PDetailsMessage from "@/components/pages/commit/PDetailsMessage.vue";

const { repo, chash, reference, title, significances } = defineProps<{
  repo: string;
  chash: string;
  reference: string;
  title: string;
  significances: JsonSignificance[];
}>();

const markers = { 1: "+", [-1]: "-", 0: "~" };
const colors = { 1: "text-green", [-1]: "text-red", 0: undefined };
</script>

<template>
  <template v-if="significances.length > 0">
    <div>{{ title }} ({{ significances.length }})</div>
    <CList>
      <CListItem
        v-for="(significance, i) in significances"
        :key="i"
        :marker="markers[significance.goodness]"
        :class="colors[significance.goodness]"
      >
        <PDetailsMessage :repo :chash :reference :significance />
      </CListItem>
    </CList>
  </template>
</template>
