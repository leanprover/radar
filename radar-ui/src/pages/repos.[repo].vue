<script setup lang="ts">
import { useRoute } from "vue-router";
import { useRepoHistory } from "@/composables/useRepoHistory.ts";
import { computed, reactive } from "vue";
import { useRepos } from "@/composables/useRepos.ts";
import CLoading from "@/components/CLoading.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CSection from "@/components/CSection.vue";
import CLinkCommit from "@/components/CLinkCommit.vue";

const route = useRoute("/repos.[repo]");
const repos = reactive(useRepos());
const history = reactive(useRepoHistory(() => route.params.repo));

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

  <CLoading v-if="!history.isSuccess" :error="history.error" />
  <CSection v-else>
    <CSectionTitle>Recent commits</CSectionTitle>
    <div class="flex flex-col">
      <div v-for="commit in history.data.commits" :key="commit.chash" class="flex gap-2">
        <div>*</div>
        <CLinkCommit
          :repo="route.params.repo"
          :chash="commit.chash"
          :title="commit.title"
          :author="commit.author.name"
          :time="commit.committer.time"
        />
      </div>
    </div>
  </CSection>
</template>
