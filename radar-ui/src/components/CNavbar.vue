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

function setSelectedToRoute() {
  const repo = route.params["repo"];
  if (typeof repo !== "string") return;
  if (selected.value === repo) return;
  selected.value = repo;
}

function setRouteToSelected() {
  const repo = route.params["repo"];
  if (typeof repo !== "string") return;
  if (selected.value === repo) return;
  const params = Object.assign({}, route.params, { repo: selected.value });
  router.push({ name: route.name, params: params });
}

watch(route, () => setSelectedToRoute());
watch(selected, () => setRouteToSelected());
setSelectedToRoute();
</script>

<template>
  <div class="flex items-center border-b p-2">
    <CNavbarRepoSelect v-model="selected"></CNavbarRepoSelect>
    <div class="flex grow justify-end"><CColorMode></CColorMode></div>
  </div>
</template>
