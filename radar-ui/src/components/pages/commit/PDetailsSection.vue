<script setup lang="ts">
import type { JsonMessage, JsonMessageGoodness } from "@/api/types.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import PDetailsMessage from "@/components/pages/commit/PDetailsMessage.vue";
import { computed } from "vue";

const { repo, chash, reference, title, messages } = defineProps<{
  repo: string;
  chash: string;
  reference: string;
  title: string;
  messages: JsonMessage[];
}>();

function marker(goodness: JsonMessageGoodness) {
  if (goodness === "GOOD") return "+";
  if (goodness === "BAD") return "-";
  return "~";
}

function color(goodness: JsonMessageGoodness) {
  if (goodness === "GOOD") return "text-green";
  if (goodness === "BAD") return "text-red";
  return undefined;
}

const counters = computed(() => {
  const good = messages.filter((it) => it.goodness === "GOOD").length;
  const bad = messages.filter((it) => it.goodness === "BAD").length;
  const neutral = messages.length - good - bad;

  const elements = [];
  if (good > 0) elements.push({ text: `${marker("GOOD")}${good.toFixed()}`, cls: color("GOOD") });
  if (bad > 0) elements.push({ text: `${marker("BAD")}${bad.toFixed()}`, cls: color("BAD") });
  if (neutral > 0) elements.push({ text: neutral.toFixed(), cls: color("NEUTRAL") });
  return elements;
});

const visible = computed(() => messages.filter((it) => !it.hidden));
const hidden = computed(() => messages.filter((it) => it.hidden));
</script>

<template>
  <template v-if="messages.length > 0">
    <div>
      {{ title }}
      (<template v-for="(counter, index) in counters" :key="index"
        ><template v-if="index > 0">, </template><span :class="counter.cls">{{ counter.text }}</span></template
      >)
    </div>

    <CList>
      <CListItem
        v-for="(message, i) in visible"
        :key="i"
        :marker="marker(message.goodness)"
        :class="color(message.goodness)"
      >
        <PDetailsMessage :repo :chash :reference :message />
      </CListItem>
    </CList>

    <details v-if="hidden.length > 0">
      <summary class="cursor-pointer select-none">and {{ hidden.length }} hidden</summary>

      <CList>
        <CListItem
          v-for="(message, i) in hidden"
          :key="i"
          :marker="marker(message.goodness)"
          :class="color(message.goodness)"
        >
          <PDetailsMessage :repo :chash :reference :message />
        </CListItem>
      </CList>
    </details>
  </template>
</template>
