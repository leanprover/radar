<script setup lang="ts">
import CColorMode from "@/components/CColorMode.vue";
import CRepoSelect from "@/components/CRepoSelect.vue";
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();
const selected = ref<string>();

// Two-way binding between the route and the repo selector.
// For any route which includes exactly one param named "repo",
// said param will be synchronized with the repo selector.
watch(route, () => {
  const repo = route.params["repo"];
  if (typeof repo !== "string") return;
  if (selected.value === repo) return;
  selected.value = repo;
});
watch(selected, () => {
  const repo = route.params["repo"];
  if (typeof repo !== "string") return;
  if (selected.value === repo) return;
  const params = Object.assign({}, route.params, { repo: selected.value });
  router.push({ name: route.name, params: params });
});
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <div class="flex items-center border-b p-2">
      <CRepoSelect v-model="selected"></CRepoSelect>
      <div class="flex grow justify-end"><CColorMode></CColorMode></div>
    </div>
    <RouterView />
  </div>
</template>
