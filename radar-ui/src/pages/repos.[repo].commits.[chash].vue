<script setup lang="ts">
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { useRoute } from "vue-router";
import { reactive } from "vue";
import { useCommitInfo } from "@/composables/useCommitInfo.ts";
import CSkeleton from "@/components/CSkeleton.vue";
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
    <CardContent v-if="!(commit.isSuccess && !commit.data.body)" class="flex flex-col gap-2">
      <CSkeleton v-if="!commit.isSuccess" :error="commit.error" class="h-[100px] w-[80ch]" />
      <pre v-else-if="commit.data.body" class="border-l-4 py-1 pl-3 text-sm whitespace-pre-wrap">{{
        commit.data.body
      }}</pre>
    </CardContent>
    <CardFooter v-if="commit.isSuccess" class="flex flex-col items-stretch">
      <div class="flex gap-8">
        <div class="flex flex-1 flex-col items-start gap-1">
          <RouterLink
            v-for="parent in commit.data.parents"
            :key="parent.chash"
            :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: parent.chash } }"
            :class="
              cn('flex hover:underline', {
                'text-muted-foreground': !parent.tracked,
              })
            "
          >
            <ChevronLeftIcon :size="20" class="relative top-1/16 shrink-0" />
            <div class="line-clamp-1 text-sm break-all">{{ parent.title }}</div>
          </RouterLink>
        </div>
        <div class="flex flex-1 flex-col items-end gap-1">
          <RouterLink
            v-for="child in commit.data.children"
            :key="child.chash"
            :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: child.chash } }"
            :class="
              cn('flex pl-2 hover:underline', {
                'text-muted-foreground': !child.tracked,
              })
            "
          >
            <div class="line-clamp-1 text-sm break-all">{{ child.title }}</div>
            <ChevronRightIcon :size="20" class="relative top-1/16 shrink-0" />
          </RouterLink>
        </div>
      </div>
    </CardFooter>
  </Card>
</template>
