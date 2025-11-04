<script setup lang="ts">
import { useRepoGithubBot } from "@/api/repoGithubBot.ts";
import { useRepos } from "@/api/repos.ts";
import CLoading from "@/components/CLoading.vue";
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
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
  <CSection v-else>
    <CSectionTitle>
      Repo {{ route.params.repo }}
      <template v-if="info" #subtitle>{{ info.description }}</template>
      <template v-else #subtitle>This repo does not exist.</template>
    </CSectionTitle>
    <div v-if="info">
      Source:
      <a class="cursor-pointer hover:underline" :href="info.url" target="_blank">{{ info.url }}</a>
    </div>
  </CSection>

  <CSection>
    <CSectionTitle>Recent commits</CSectionTitle>
    <PHistory :repo="route.params.repo" />
  </CSection>

  <CSection v-if="github.isSuccess && github.data.commands.length > 0">
    <CSectionTitle>Recent GitHub bot commands</CSectionTitle>
    <PHistoryGithub :repo="route.params.repo" :commands="github.data.commands" />
  </CSection>
</template>
