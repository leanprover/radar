<script setup lang="ts">
import { useRoute } from "vue-router";
import { useRepoHistory } from "@/composables/useRepoHistory.ts";
import { computed, reactive } from "vue";
import { useRepos } from "@/composables/useRepos.ts";
import CLoading from "@/components/CLoading.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CTimeAgo from "@/components/CTimeAgo.vue";

const route = useRoute("/repos.[repo]");
const repos = reactive(useRepos());
const history = reactive(useRepoHistory(() => route.params.repo));

const info = computed(() => repos.data?.repos.find((it) => it.name === route.params.repo));
</script>

<template>
  <CLoading v-if="!repos.isSuccess" :error="repos.error" />
  <div v-else class="flex flex-col">
    <CSectionTitle>
      Repo {{ route.params.repo }}
      <template v-if="info" #subtitle>{{ info.description }}</template>
      <template v-else #subtitle>This repo does not exist.</template>
    </CSectionTitle>
    <div v-if="info">
      Source:
      <a class="cursor-pointer hover:underline" :href="info.url" target="_blank">{{ info.url }}</a>
    </div>
  </div>

  <CLoading v-if="!history.isSuccess" :error="history.error" />
  <div v-else class="flex flex-col">
    <CSectionTitle>Recent commits</CSectionTitle>
    <div v-for="commit in history.data.commits" :key="commit.chash" class="flex gap-2">
      <div>*</div>
      <div class="flex flex-wrap items-baseline gap-x-2">
        <RouterLink
          :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: commit.chash } }"
          class="hover:underline"
        >
          {{ commit.title }}
        </RouterLink>
        <div class="text-foreground-alt text-xs">
          <CTimeAgo :when="commit.committerTime" class="hover:text-foreground" />
          by {{ commit.author }}
        </div>
      </div>
    </div>
  </div>
</template>
