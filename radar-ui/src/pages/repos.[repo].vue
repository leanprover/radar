<script setup lang="ts">
import { useRoute } from "vue-router";
import { useRepoHistory } from "@/composables/useRepoHistory.ts";
import { computed, reactive } from "vue";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import { useRepos } from "@/composables/useRepos.ts";
import CLoading from "@/components/CLoading.vue";
import CHeading2 from "@/components/CHeading2.vue";

const route = useRoute("/repos.[repo]");
const repos = reactive(useRepos());
const history = reactive(useRepoHistory(() => route.params.repo));

const info = computed(() => repos.data?.repos.find((it) => it.name === route.params.repo));
</script>

<template>
  <CLoading v-if="!repos.isSuccess" :error="repos.error" />
  <div v-else class="flex flex-col">
    <div class="mb-2 flex flex-col">
      <h1 class="text-lg font-bold">{{ route.params.repo }}</h1>
      <div class="text-foreground-alt text-xs">
        <template v-if="info">{{ info.description }}</template>
        <template v-else>This repo does not exist.</template>
      </div>
    </div>
    <div v-if="info">
      Repo:
      <a class="cursor-pointer hover:underline" :href="info.url" target="_blank">{{ info.url }}</a>
    </div>
  </div>

  <CLoading v-if="!history.isSuccess" :error="history.error" />
  <div v-else class="flex flex-col">
    <CHeading2>Recent commits</CHeading2>
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
          <span :title="useDateFormat(commit.committerTime, 'YYYY-MM-DD HH:mm:ss').value" class="hover:text-foreground">
            {{ useTimeAgo(commit.committerTime) }}
          </span>
          by {{ commit.author }}
        </div>
      </div>
    </div>
  </div>
</template>
