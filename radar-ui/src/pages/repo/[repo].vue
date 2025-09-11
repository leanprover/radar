<script setup lang="ts">
import { useRoute } from "vue-router";
import { useRepoInfo } from "@/composables/useRepoInfo.ts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { GitHubIcon } from "vue3-simple-icons";
import { useRepoHistory } from "@/composables/useRepoHistory.ts";
import { reactive } from "vue";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import { GitCommitVerticalIcon, EllipsisVerticalIcon } from "lucide-vue-next";

const route = useRoute("/repo/[repo]");
const info = useRepoInfo(() => route.params.repo);
const history = reactive(useRepoHistory(() => route.params.repo));
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>{{ route.params.repo }}</CardTitle>
      <CardDescription>{{ info?.description ?? "This repo does not exist." }}</CardDescription>
    </CardHeader>
    <CardContent>
      <div class="flex flex-col items-start">
        <a
          v-if="info !== undefined"
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

  <Card>
    <CardHeader>
      <CardTitle>Recent commits</CardTitle>
      <CardDescription>Freshly cherry-picked</CardDescription>
    </CardHeader>
    <CardContent>
      <div v-if="!history.isSuccess" class="text-muted-foreground italic">Loading...</div>
      <div v-else class="flex flex-col gap-4">
        <RouterLink
          to="/"
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
