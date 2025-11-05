<script setup lang="ts">
import { useRepoGithubBot } from "@/api/repoGithubBot.ts";
import { useRepos } from "@/api/repos.ts";
import CLoading from "@/components/CLoading.vue";
import CSection from "@/components/CSection.vue";
import PHistory from "@/components/pages/repo/PHistory.vue";
import PHistoryGithub from "@/components/pages/repo/PHistoryGithub.vue";
import { computed, reactive } from "vue";
import { useRoute } from "vue-router";

const route = useRoute("/repos.[repo]");
const repos = reactive(useRepos());
const github = reactive(useRepoGithubBot(() => route.params.repo));

const info = computed(() => repos.data?.repos.find((it) => it.name === route.params.repo));
</script>

<template>
  <CLoading v-if="!repos.isSuccess" :error="repos.error" />
  <CSection v-else :title="`Repo ${route.params.repo}`" :subtitle="info?.description ?? 'This repo does not exist.'">
    <div v-if="info">
      Source:
      <a class="cursor-pointer hover:underline" :href="info.url" target="_blank">{{ info.url }}</a>
    </div>
  </CSection>

  <CSection title="Recent commits">
    <PHistory :repo="route.params.repo" />
  </CSection>

  <CSection v-if="github.isSuccess && github.data.commands.length > 0" title="Recent GitHub bot commands">
    <PHistoryGithub :repo="route.params.repo" :commands="github.data.commands" />
  </CSection>
</template>
