<script setup lang="ts">
import CColorMode from "@/components/CColorMode.vue";
import CRepoSelect from "@/components/CRepoSelect.vue";
import { ref } from "vue";

const selected = ref<string>();
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <div class="flex border-b p-2">
      <CRepoSelect v-model="selected"></CRepoSelect>
      <div class="flex grow justify-end"><CColorMode></CColorMode></div>
    </div>
    <div
      v-if="!selected"
      class="text-muted-foreground bg-muted flex grow items-center justify-center text-xl font-light italic"
    >
      No repo selected
    </div>
  </div>
</template>
