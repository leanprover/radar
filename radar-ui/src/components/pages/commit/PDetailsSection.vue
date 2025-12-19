<script setup lang="ts">
import type { JsonSignificance } from "@/api/types.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import PDetailsMessage from "@/components/pages/commit/PDetailsMessage.vue";
import { computed } from "vue";

const { repo, chash, reference, title, significances } = defineProps<{
  repo: string;
  chash: string;
  reference: string;
  title: string;
  significances: JsonSignificance[];
}>();

function marker(goodness: number) {
  if (goodness > 0) return "+";
  if (goodness < 0) return "-";
  return "~";
}

function color(goodness: number) {
  if (goodness > 0) return "text-green";
  if (goodness < 0) return "text-red";
  return undefined;
}

const counters = computed(() => {
  const good = significances.filter((it) => it.goodness > 0).length;
  const bad = significances.filter((it) => it.goodness < 0).length;
  const neutral = significances.filter((it) => it.goodness === 0).length;

  const elements = [];
  if (good > 0) elements.push({ text: `${marker(1)}${good.toFixed()}`, cls: color(1) });
  if (bad > 0) elements.push({ text: `${marker(-1)}${bad.toFixed()}`, cls: color(-1) });
  if (neutral > 0) elements.push({ text: neutral.toFixed(), cls: color(0) });
  return elements;
});
</script>

<template>
  <template v-if="significances.length > 0">
    <div>
      {{ title }}
      (<template v-for="(counter, index) in counters" :key="index"
        ><template v-if="index > 0">, </template><span :class="counter.cls">{{ counter.text }}</span></template
      >)
    </div>
    <CList>
      <CListItem
        v-for="(significance, i) in significances"
        :key="i"
        :marker="marker(significance.goodness)"
        :class="color(significance.goodness)"
      >
        <PDetailsMessage :repo :chash :reference :significance />
      </CListItem>
    </CList>
  </template>
</template>
