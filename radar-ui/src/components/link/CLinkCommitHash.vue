<script setup lang="ts">
import CLink from "@/components/link/CLink.vue";
import CLinkExternal from "@/components/link/CLinkExternal.vue";

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
    <CLink v-if="repo !== undefined"
      ><RouterLink :to="{ name: '/repos.[repo].commits.[chash]', params: { repo, chash }, query: { s: queryS } }">{{
        chash
      }}</RouterLink></CLink
    >
    <span v-else>{{ chash }}</span>
    <span v-if="url !== undefined && chashAgainst !== undefined">
      (<CLinkExternal :href="linkSource(url, chash)">source</CLinkExternal>,
      <CLinkExternal :href="linkDiff(url, chashAgainst, chash)">diff</CLinkExternal>)</span
    >
    <span v-else-if="url !== undefined">(<CLinkExternal :href="linkSource(url, chash)">source</CLinkExternal>)</span>
  </span>
</template>
