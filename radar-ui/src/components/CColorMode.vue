<script setup lang="ts">
import { useColorMode } from "@vueuse/core";
import { MoonIcon, SunIcon, SunMoonIcon } from "lucide-vue-next";
import { Button } from "@/components/ui/button";

const { store } = useColorMode();

const next = { auto: "dark" as const, dark: "light" as const, light: "auto" as const };
</script>

<template>
  <Button variant="ghost" size="icon" @click="store = next[store]">
    <SunMoonIcon v-if="store === 'auto'" />
    <MoonIcon v-if="store === 'dark'" />
    <SunIcon v-if="store === 'light'" />
  </Button>
</template>
