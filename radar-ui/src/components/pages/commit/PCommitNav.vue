<script setup lang="ts">
import type { JsonLinkedCommit } from "@/api/commit.ts";
import CLinkExternal from "@/components/link/CLinkExternal.vue";
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
    <CLinkExternal v-if="lakeprofReportUrl" :href="lakeprofReportUrl + chash" class="col-span-full">
      Lakeprof report
    </CLinkExternal>

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
