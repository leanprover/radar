<script setup lang="ts">
import { useRepoStore } from "@/stores/useRepoStore.ts";
import { useRouter } from "vue-router";

const router = useRouter();
const repoStore = useRepoStore();

if (repoStore.selected !== undefined) {
  void router.push({ name: "/repos.[repo]", params: { repo: repoStore.selected } });
}
</script>

<template>
  <div class="text-foreground-alt italic">No repo selected</div>
</template>
