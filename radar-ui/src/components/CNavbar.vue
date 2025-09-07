<script setup lang="ts">
import CColorMode from "@/components/CColorMode.vue";
import CNavbarRepoSelect from "@/components/CNavbarRepoSelect.vue";
import { useRoute, useRouter } from "vue-router";
import { ref, watch } from "vue";

const router = useRouter();
const route = useRoute();
const selected = ref<string>();

// Two-way binding between the route and the repo selector.
// For any route which includes exactly one param named "repo",
// said param will be synchronized with the repo selector.

function getRepoParam(): string | undefined {
  if ("repo" in route.params) return route.params.repo;
  return undefined;
}

function setSelectedToRoute() {
  const repo = getRepoParam();
  if (repo === undefined) {
    // The route doesn't have a "repo" param, so we're just going to do nothing.
    // We don't want to clear the selected repo when looking at the queue
    // or other non-repo-related routes.
    return;
  }

  if (selected.value === repo) return;
  selected.value = repo;
}

function setRouteToSelected() {
  if (selected.value === undefined) {
    // This shouldn't normally happen, so we're just going to ignore it.
    return;
  }

  const repo = getRepoParam();
  if (repo === undefined) {
    // Our route doesn't contain a "repo" param.
    router.push({ name: "/repo/[repo]", params: { repo: selected.value } });
    return;
  }

  if (selected.value === repo) return;

  const params = Object.assign({}, route.params, { repo: selected.value });
  router.push({ name: route.name, params: params });
}

watch(route, () => setSelectedToRoute());
watch(selected, () => setRouteToSelected());
setSelectedToRoute();
</script>

<template>
  <div class="flex items-center gap-4 border-b p-2">
    <CNavbarRepoSelect v-model="selected"></CNavbarRepoSelect>

    <RouterLink v-if="selected !== undefined" :to="{ name: '/repo/[repo]', params: { repo: selected } }">
      Overview
    </RouterLink>
    <div v-else class="text-muted-foreground">Overview</div>

    <RouterLink class="hover:underline" :to="{ name: '/queue' }">Queue</RouterLink>

    <div class="flex grow justify-end">
      <CColorMode />
    </div>
  </div>
</template>
