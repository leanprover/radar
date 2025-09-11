<script setup lang="ts">
import { useRoute } from "vue-router";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { GitHubIcon } from "vue3-simple-icons";
import { useRepoHistory } from "@/composables/useRepoHistory.ts";
import { computed, reactive } from "vue";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import { EllipsisVerticalIcon, GitCommitVerticalIcon } from "lucide-vue-next";
import { useRepos } from "@/composables/useRepos.ts";
import CSkeleton from "@/components/ui/CSkeleton.vue";

const route = useRoute("/repos.[repo]");
const repos = reactive(useRepos());
const history = reactive(useRepoHistory(() => route.params.repo));

const info = computed(() => repos.data?.repos.find((it) => it.name === route.params.repo));
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>
        <CSkeleton v-if="!repos.isSuccess" :is-error="repos.isError" class="h-4 w-[8ch]" />
        <template v-else>{{ route.params.repo }}</template>
      </CardTitle>
      <CardDescription>
        <CSkeleton v-if="!repos.isSuccess" :is-error="repos.isError" class="my-1 h-3 w-[24ch] pt-2" />
        <template v-else-if="info">{{ info.description }}</template>
        <template v-else>This repo does not exist.</template>
      </CardDescription>
    </CardHeader>
    <CardContent>
      <div class="flex flex-col items-start">
        <template v-if="repos.isSuccess && !info" />
        <CSkeleton v-else-if="!repos.isSuccess || !info" :is-error="repos.isError" class="h-7 w-[30ch] border" />
        <a
          v-else
          class="flex gap-2 rounded-md border px-2 py-1 text-sm hover:underline"
          :href="info.url"
          target="_blank"
        >
          <GitHubIcon v-if="/^https?:\/\/github.com\//.test(info.url)" class="size-5" />
          {{ info.url }}
        </a>
      </div>
    </CardContent>
  </Card>

  <Card v-if="!(repos.isSuccess && !info)">
    <CardHeader>
      <CardTitle>Recent commits</CardTitle>
      <CardDescription>Freshly cherry-picked</CardDescription>
    </CardHeader>
    <CardContent>
      <div v-if="!history.isSuccess" class="flex items-center gap-2">
        <CSkeleton :is-error="history.isError" class="h-8 w-8 rounded-full" />
        <div class="flex flex-col">
          <CSkeleton :is-error="history.isError" class="my-1 h-4 w-[30ch]" />
          <CSkeleton :is-error="history.isError" class="my-1 h-3 w-[20ch]" />
        </div>
      </div>
      <div v-else class="flex flex-col gap-4">
        <RouterLink
          :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: commit.chash } }"
          v-for="commit in history.data.commits"
          :key="commit.chash"
          class="group flex items-center gap-2"
        >
          <GitCommitVerticalIcon :size="32" />
          <div class="flex flex-col">
            <div class="group-hover:underline">{{ commit.title }}</div>
            <div class="text-muted-foreground text-sm">
              <span :title="useDateFormat(commit.committerTime * 1000, 'YYYY-MM-DD HH:mm:ss').value">
                {{ useTimeAgo(commit.committerTime * 1000) }}
              </span>
              by
              <template v-if="commit.author === commit.committer">{{ commit.author }}</template>
              <template v-else>{{ commit.author }}, {{ commit.committer }}</template>
            </div>
          </div>
        </RouterLink>
        <EllipsisVerticalIcon :size="32" v-if="history.data.nextAt !== null" />
      </div>
    </CardContent>
  </Card>
</template>
