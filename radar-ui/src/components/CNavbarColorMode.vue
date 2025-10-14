<script setup lang="ts">
import CNavbarButton from "@/components/CNavbarButton.vue";
import { useColorMode } from "@vueuse/core";

const { store } = useColorMode();

const next = {
  auto: "dark" as const,
  dark: "light" as const,
  light: "auto" as const,
};
</script>

<template>
  <CNavbarButton title="Change color mode" @click="store = next[store]">{{ store }}</CNavbarButton>
</template>
