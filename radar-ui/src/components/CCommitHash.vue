<script setup lang="ts">
const { url = undefined, chash } = defineProps<{
  url?: string;
  chash: string;
}>();

// Assumes a Github-like URL schema
function href(url: string, chash: string): string {
  const slash = url.endsWith("/") ? "" : "/";
  return `${url}${slash}commit/${chash}`;
}
</script>

<template>
  <span v-if="url === undefined">{{ chash }}</span>
  <a v-else :href="href(url, chash)" target="_blank" class="hover:underline">{{ chash }}</a>
</template>
