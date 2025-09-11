<script setup lang="ts">
import CColorMode from "@/components/CColorMode.vue";
import CNavbarRepoSelect from "@/components/CNavbarRepoSelect.vue";
import { useRoute, useRouter } from "vue-router";
import { watch } from "vue";
import { Button } from "@/components/ui/button";
import { useRepoStore } from "@/stores/useRepoStore.ts";

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
  if (repoParam === undefined) {
    // Our route doesn't contain a "repo" param.
    router.push({ name: "/repos.[repo]", params: { repo: repo.selected } });
    return;
  }

  if (repo.selected === repoParam) return;

  const params = Object.assign({}, route.params, { repo: repo.selected });
  router.push({ name: route.name, params } as any);
}

watch(route, () => setSelectedToRoute());
watch(repo, () => setRouteToSelected());
setSelectedToRoute();
</script>

<template>
  <div class="bg-background flex items-center gap-2 border-b p-2">
    <CNavbarRepoSelect v-model="repo.selected"></CNavbarRepoSelect>

    <RouterLink v-if="repo.selected" :to="{ name: '/repos.[repo]', params: { repo: repo.selected } }">
      <Button :variant="route.name === '/repos.[repo]' ? 'secondary' : 'ghost'">Overview</Button>
    </RouterLink>
    <Button v-else variant="ghost" disabled>Overview</Button>

    <RouterLink :to="{ name: '/queue' }">
      <Button :variant="route.name === '/queue' ? 'secondary' : 'ghost'">Queue</Button>
    </RouterLink>

    <div class="flex grow justify-end">
      <CColorMode />
    </div>
  </div>
</template>
