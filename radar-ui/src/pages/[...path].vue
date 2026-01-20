<script setup lang="ts">
import { radarTitle } from "@/lib/utils.ts";
import { useTitle } from "@vueuse/core";

useTitle(radarTitle("404"));
</script>

<template>
  <div class="text-red font-bold">Page not found</div>
</template>
