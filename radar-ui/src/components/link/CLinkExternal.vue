<script setup lang="ts">
import CLink from "@/components/link/CLink.vue";

const { href } = defineProps<{ href: string }>();
</script>

<template>
  <CLink class="decoration-blue decoration-solid hover:decoration-wavy"
    ><a :href target="_blank"><slot /></a
  ></CLink>
</template>
