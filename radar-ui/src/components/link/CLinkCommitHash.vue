<script setup lang="ts">
const {
  repo = undefined,
  url = undefined,
  chash,
  chashAgainst = undefined,
  queryS = undefined,
} = defineProps<{
  repo?: string;
  url?: string;
  chash: string;
  chashAgainst?: string;
  queryS?: string;
}>();

// Assuming a Github-like URL schema
function linkSource(url: string, chash: string): string {
  const slash = url.endsWith("/") ? "" : "/";
  return `${url}${slash}commit/${chash}`;
}
function linkDiff(url: string, first: string, second: string): string {
  const slash = url.endsWith("/") ? "" : "/";
  return `${url}${slash}compare/${first}...${second}`;
}
</script>

<template>
  <span>
    <RouterLink
      v-if="repo !== undefined"
      :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash }, query: { s: queryS } }"
      class="hover:underline"
    >
      {{ chash }}
    </RouterLink>
    <span v-else>{{ chash }}</span>
    <span v-if="url !== undefined && chashAgainst !== undefined">
      (<a :href="linkSource(url, chash)" target="_blank" class="hover:underline">source</a>,
      <a :href="linkDiff(url, chashAgainst, chash)" target="_blank" class="hover:underline">diff</a>)</span
    >
    <span v-else-if="url !== undefined">
      (<a :href="linkSource(url, chash)" target="_blank" class="hover:underline">source</a>)</span
    >
  </span>
</template>
