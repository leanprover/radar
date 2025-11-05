<script setup lang="ts">
import CSectionTitle from "@/components/CSectionTitle.vue";
import { CollapsibleContent, CollapsibleRoot, CollapsibleTrigger } from "reka-ui";
import { ref } from "vue";

const {
  title,
  subtitle = undefined,
  collapsible = false,
  startOpen = false,
} = defineProps<{
  title: string;
  subtitle?: string;
  collapsible?: boolean;
  startOpen?: boolean;
}>();

const open = ref(startOpen);
</script>

<template>
  <CollapsibleRoot
    v-if="collapsible"
    v-model:open="open"
    class="border-background-alt flex flex-col gap-2 border-l-2 px-2"
  >
    <CollapsibleTrigger class="cursor-pointer text-left">
      <div :class="{ 'ml-3 list-item': true, 'list-[disclosure-open]': open, 'list-[disclosure-closed]': !open }">
        <CSectionTitle :title :subtitle />
      </div>
    </CollapsibleTrigger>
    <CollapsibleContent class="CollapsibleContent flex flex-col gap-2">
      <slot />
    </CollapsibleContent>
  </CollapsibleRoot>
  <div v-else class="border-background-alt flex flex-col gap-2 border-l-2 px-2">
    <CSectionTitle :title :subtitle />
    <slot />
  </div>
</template>

<style>
.CollapsibleContent {
  overflow: hidden;
}

.CollapsibleContent[data-state="open"] {
  animation: slideDown 200ms ease-out;
}

.CollapsibleContent[data-state="closed"] {
  animation: slideUp 200ms ease-out;
}

@keyframes slideDown {
  from {
    height: 0;
  }
  to {
    height: var(--reka-collapsible-content-height);
  }
}

@keyframes slideUp {
  from {
    height: var(--reka-collapsible-content-height);
  }
  to {
    height: 0;
  }
}
</style>
