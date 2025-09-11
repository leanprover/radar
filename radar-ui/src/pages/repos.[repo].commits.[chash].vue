<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useRoute } from "vue-router";
import { reactive } from "vue";
import { useCommitInfo } from "@/composables/useCommitInfo.ts";
import CSkeleton from "@/components/ui/CSkeleton.vue";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import { cn } from "@/lib/utils.ts";
import { ChevronLeftIcon, ChevronRightIcon } from "lucide-vue-next";

const route = useRoute("/repos.[repo].commits.[chash]");
const commit = reactive(
  useCommitInfo(
    () => route.params.repo,
    () => route.params.chash,
  ),
);
</script>

<template>
  <div class="flex flex-col gap-2">
    <Card>
      <CardHeader>
        <CardTitle>
          <CSkeleton v-if="!commit.isSuccess" :error="commit.error" class="h-4 w-[30ch]" />
          <template v-else>{{ commit.data.title }}</template>
        </CardTitle>
        <CardDescription>
          <CSkeleton v-if="!commit.isSuccess" :error="commit.error" class="my-1 h-3 w-[24ch] pt-2" />
          <template v-else>
            <span :title="useDateFormat(commit.data.committer.time, 'YYYY-MM-DD HH:mm:ss').value">
              {{ useTimeAgo(commit.data.committer.time) }}
            </span>
            by {{ commit.data.author.name }} &lt;{{ commit.data.author.email }}&gt;
          </template>
        </CardDescription>
      </CardHeader>
      <CardContent class="flex flex-col gap-2">
        <CSkeleton v-if="!commit.isSuccess" :error="commit.error" class="h-[100px] w-[80ch]" />
        <pre class="text-sm whitespace-pre-wrap" v-else-if="commit.data.body">{{ commit.data.body }}</pre>
      </CardContent>
    </Card>
    <div class="flex gap-8" v-if="commit.isSuccess">
      <div class="flex flex-1 flex-col items-start gap-1">
        <RouterLink
          v-for="commit in commit.data.parents"
          :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: commit.chash } }"
          :class="
            cn('bg-background flex items-center rounded-md border pr-2 hover:underline', {
              'text-muted-foreground': !commit.tracked,
            })
          "
        >
          <ChevronLeftIcon />
          <span class="line-clamp-1 text-sm break-all">{{ commit.title }}</span>
        </RouterLink>
      </div>
      <div class="flex flex-1 flex-col items-end gap-1">
        <RouterLink
          v-for="commit in commit.data.children"
          :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: commit.chash } }"
          :class="
            cn('bg-background flex items-center rounded-md border pl-2 hover:underline', {
              'text-muted-foreground': !commit.tracked,
            })
          "
        >
          <span class="line-clamp-1 text-sm break-all">{{ commit.title }}</span>
          <ChevronRightIcon />
        </RouterLink>
      </div>
    </div>
  </div>
</template>
