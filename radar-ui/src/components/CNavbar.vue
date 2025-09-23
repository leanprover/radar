<script setup lang="ts">
import CNavbarColorMode from "@/components/CNavbarColorMode.vue";
import CNavbarRepoSelect from "@/components/CNavbarRepoSelect.vue";
import { useRoute, useRouter } from "vue-router";
import { watch } from "vue";
import { useRepoStore } from "@/stores/useRepoStore.ts";
import CNavbarButton from "@/components/CNavbarButton.vue";

const router = useRouter();
const route = useRoute();
const repo = useRepoStore();

// Two-way binding between the route and the repo selector.
// For any route which includes exactly one param named "repo",
// said param will be synchronized with the repo selector.

function getRepoParam(): string | undefined {
  if ("repo" in route.params) return route.params.repo;
  return undefined;
}

function setSelectedToRoute() {
  const repoParam = getRepoParam();
  if (repoParam === undefined) {
    // The route doesn't have a "repo" param, so we're just going to do nothing.
    // We don't want to clear the selected repo when looking at the queue
    // or other non-repo-related routes.
    return;
  }

  if (repo.selected === repoParam) return;
  repo.selected = repoParam;
}

function setRouteToSelected() {
  if (repo.selected === undefined) {
    // This shouldn't normally happen, so we're just going to ignore it.
    return;
  }

  const repoParam = getRepoParam();
  if (repoParam === repo.selected) return;

  void router.push({ name: "/repos.[repo]", params: { repo: repo.selected } });
  return;
}

watch(route, () => {
  setSelectedToRoute();
});
watch(repo, () => {
  setRouteToSelected();
});
setSelectedToRoute();
</script>

<template>
  <div class="flex gap-1 border-b pr-1">
    <CNavbarRepoSelect v-model="repo.selected"></CNavbarRepoSelect>

    <RouterLink v-if="repo.selected" :to="{ name: '/repos.[repo]', params: { repo: repo.selected } }">
      <CNavbarButton :selected="route.name === '/repos.[repo]'">Overview</CNavbarButton>
    </RouterLink>
    <CNavbarButton v-else disabled>Overview</CNavbarButton>

    <RouterLink :to="{ name: '/queue' }">
      <CNavbarButton :selected="route.name === '/queue'">Queue</CNavbarButton>
    </RouterLink>

    <div class="flex grow justify-end">
      <CNavbarColorMode />
    </div>
  </div>
</template>
