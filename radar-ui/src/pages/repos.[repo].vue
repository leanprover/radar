<script setup lang="ts">
import { postAdminVacuum } from "@/api/adminVacuum.ts";
import CButton from "@/components/CButton.vue";
import CLinkCommit from "@/components/CLinkCommit.vue";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CLoading from "@/components/CLoading.vue";
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import CTimeAgo from "@/components/CTimeAgo.vue";
import { useRepoGithubBot } from "@/composables/useRepoGithubBot.ts";
import { useRepoHistory } from "@/composables/useRepoHistory.ts";
import { useRepos } from "@/composables/useRepos.ts";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { computed, reactive } from "vue";
import { useRoute } from "vue-router";

const route = useRoute("/repos.[repo]");
const admin = useAdminStore();
const repos = reactive(useRepos());
const history = reactive(useRepoHistory(() => route.params.repo));
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

  <CSection v-if="admin.token !== undefined">
    <CSectionTitle>Admin</CSectionTitle>
    <div class="bg-background-alt flex max-w-[80ch] flex-col gap-2 p-1">
      <div class="flex"><CButton @click="postAdminVacuum(admin.token, route.params.repo)">Vacuum</CButton></div>
      <details>
        <summary>Explanation</summary>
        <div class="mt-2">
          Vacuum the SQLite database. This should not be necessary under normal circumstances. It might be useful once
          we've amassed a few gigabytes of data and want to defragment the db for performance reasons.
        </div>
      </details>
    </div>
  </CSection>

  <CLoading v-if="!history.isSuccess" :error="history.error" />
  <CSection v-else>
    <CSectionTitle>Recent commits</CSectionTitle>
    <CList>
      <CListItem v-for="commit in history.data.commits" :key="commit.chash">
        <CLinkCommit
          :repo="route.params.repo"
          :chash="commit.chash"
          :title="commit.title"
          :author="commit.author.name"
          :time="commit.committer.time"
        />
      </CListItem>
    </CList>
  </CSection>

  <CSection v-if="github.isSuccess && github.data.commands.length > 0">
    <CSectionTitle>Recent GitHub bot commands</CSectionTitle>
    <CList>
      <CListItem v-for="command in github.data.commands" :key="command.url">
        <div :class="{ 'text-foreground-alt': command.completed }">
          In PR #{{ command.pr }}:
          <a :href="command.url" target="_blank" class="hover:underline">command</a>
          <template v-if="command.replyUrl"
            >,
            <a :href="command.replyUrl" target="_blank" class="hover:underline">bot reply</a>
          </template>
          <template v-if="command.completed">
            (<RouterLink
              :to="{
                name: '/repos.[repo].commits.[chash]',
                params: { repo: route.params.repo, chash: command.chash },
                query: { parent: command.againstChash },
              }"
              class="hover:underline"
              >finished</RouterLink
            >
            {{ " " }}
            <CTimeAgo :when="command.completed" />)</template
          >
        </div>
      </CListItem>
    </CList>
  </CSection>
</template>
