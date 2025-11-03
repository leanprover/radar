<script setup lang="ts">
const {
  repo = undefined,
  url = undefined,
  chash,
} = defineProps<{
  repo?: string;
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
  <span>
    <RouterLink
      v-if="repo !== undefined"
      :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash } }"
      class="hover:underline"
    >
      {{ chash }}
    </RouterLink>
    <span v-else>{{ chash }}</span>
    <span v-if="url !== undefined">
      (<a :href="href(url, chash)" target="_blank" class="hover:underline">original</a>)</span
    >
  </span>
</template>
