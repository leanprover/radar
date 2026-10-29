<script setup lang="ts">
import CButton from "@/components/CButton.vue";
import { useTimeoutFn } from "@vueuse/core";
import { ref } from "vue";

const {
  action,
  disabled = false,
  confirm = "",
} = defineProps<{
  action: () => Promise<void>;
  disabled?: boolean;
  // If set, ask the user to confirm this message before running the action.
  confirm?: string;
}>();

defineOptions({ inheritAttrs: false });

const state = ref<"idle" | "pending" | "success" | "error">("idle");
const error = ref<string>();

const { start: startResetTimer, stop: stopResetTimer } = useTimeoutFn(
  () => {
    state.value = "idle";
  },
  2000,
  { immediate: false },
);

async function onClick() {
  if (confirm && !window.confirm(confirm)) return;

  stopResetTimer();
  state.value = "pending";
  error.value = undefined;
  try {
    await action();
    state.value = "success";
    startResetTimer();
  } catch (e) {
    state.value = "error";
    error.value = e instanceof Error ? e.message : String(e);
  }
}
</script>

<template>
  <span class="inline-flex items-baseline gap-1">
    <CButton
      v-bind="$attrs"
      :disabled="disabled || state === 'pending'"
      :class="{ 'cursor-wait!': state === 'pending' }"
      @click="onClick()"
    >
      <slot />
    </CButton>
    <span v-if="state === 'pending'" class="text-foreground-alt">…</span>
    <span v-else-if="state === 'success'" class="text-green font-bold">✓</span>
    <span v-else-if="state === 'error'" :title="error" class="text-red font-bold">✗ Error</span>
  </span>
</template>
