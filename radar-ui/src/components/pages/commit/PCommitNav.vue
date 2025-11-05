<script setup lang="ts">
import type { JsonLinkedCommit } from "@/api/commit.ts";
import PCommitNavLink from "@/components/pages/commit/PCommitNavLink.vue";

const { repo, chash, search, lakeprofReportUrl } = defineProps<{
  repo: string;
  chash: string;
  search: string | undefined;
  lakeprofReportUrl: string | undefined;
  parents: JsonLinkedCommit[];
  children: JsonLinkedCommit[];
}>();
</script>

<template>
  <div class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
    <a v-if="lakeprofReportUrl" :href="lakeprofReportUrl + chash" target="_blank" class="col-span-full hover:underline">
      Lakeprof report
    </a>

    <template v-for="commit in parents" :key="commit.chash">
      <div>Parent:</div>
      <PCommitNavLink prefix="<" :repo :search :commit />
    </template>

    <template v-for="commit in children" :key="commit.chash">
      <template v-if="commit.tracked">
        <div>Child:</div>
        <PCommitNavLink prefix=">" :repo :search :commit />
      </template>
    </template>
  </div>
</template>
