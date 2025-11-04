<script setup lang="ts">
import { CollapsibleContent, CollapsibleRoot, CollapsibleTrigger } from "reka-ui";
import { ref } from "vue";
import { onBeforeRouteUpdate } from "vue-router";

const { title, body = undefined } = defineProps<{ title: string; body?: string }>();

const open = ref(false);

onBeforeRouteUpdate(() => {
  open.value = false;
});
</script>

<template>
  <CollapsibleRoot v-model:open="open" :disabled="!body" class="col-span-full ml-[4ch] flex flex-col">
    <CollapsibleTrigger :class="{ 'text-left': true, 'cursor-pointer': body }">
      <span>{{ body ? (open ? "v" : "^") : "-" }}{{ " " }}</span>
      <span class="font-bold">{{ title }}</span>
    </CollapsibleTrigger>
    <CollapsibleContent
      :class="[
        'overflow-hidden',
        'data-[state=closed]:animate-[slideUp_200ms_ease-out]',
        'data-[state=open]:animate-[slideDown_200ms_ease-out]',
      ]"
    >
      <pre class="max-w-[80ch] pt-3 whitespace-pre-wrap">{{ body }}</pre>
    </CollapsibleContent>
  </CollapsibleRoot>
</template>

<style>
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
