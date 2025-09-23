<script setup lang="ts">
import { onBeforeRouteUpdate, useRoute } from "vue-router";
import { reactive, ref } from "vue";
import { useCommitInfo } from "@/composables/useCommitInfo.ts";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import CLoading from "@/components/CLoading.vue";
import { CollapsibleContent, CollapsibleRoot, CollapsibleTrigger } from "reka-ui";
import { cn } from "@/lib/utils.ts";
import CSectionTitle from "@/components/CSectionTitle.vue";

const route = useRoute("/repos.[repo].commits.[chash]");
const commit = reactive(
  useCommitInfo(
    () => route.params.repo,
    () => route.params.chash,
  ),
);

const open = ref(false);

onBeforeRouteUpdate(() => {
  open.value = false;
});
</script>

<template>
  <CLoading v-if="!commit.isSuccess" :error="commit.error" />
  <div v-else class="grid grid-cols-[auto_1fr] gap-x-[1ch]">
    <CSectionTitle>Commit</CSectionTitle>

    <!-- TODO Link to GitHub -->
    <div class="text-yellow col-span-2">commit {{ commit.data.chash }}</div>

    <div>Author:</div>
    <div>{{ commit.data.author.name }} &lt;{{ commit.data.author.email }}&gt;</div>

    <div>Date:</div>
    <div>
      {{ useDateFormat(commit.data.author.time, "YYYY-MM-DD HH:mm:ss") }}
      ({{ useTimeAgo(commit.data.author.time) }})
    </div>

    <CollapsibleRoot
      v-model:open="open"
      :disabled="!commit.data.body"
      class="col-span-2 my-3 ml-[4ch] flex flex-col gap-3"
    >
      <CollapsibleTrigger :class="cn('text-left', { 'cursor-pointer': commit.data.body })">
        <span>{{ commit.data.body ? (open ? "v" : "^") : "-" }}{{ " " }}</span>
        <span class="font-bold">{{ commit.data.title }}</span>
      </CollapsibleTrigger>
      <CollapsibleContent class="max-w-[80ch] whitespace-pre-wrap">{{ commit.data.body }}</CollapsibleContent>
    </CollapsibleRoot>

    <template v-for="parent in commit.data.parents" :key="parent.chash">
      <div>Parent:</div>
      <RouterLink
        :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: parent.chash } }"
        :title="parent.title"
        :class="cn('cursor-pointer truncate italic hover:underline', { 'text-foreground-alt': !parent.tracked })"
      >
        &lt; {{ parent.title }}
      </RouterLink>
    </template>
    <template v-for="child in commit.data.children" :key="child.chash">
      <div>Child:</div>
      <RouterLink
        :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: child.chash } }"
        :title="child.title"
        :class="cn('cursor-pointer truncate italic hover:underline', { 'text-foreground-alt': !child.tracked })"
      >
        &gt; {{ child.title }}
      </RouterLink>
    </template>
  </div>

  <!--    <CardFooter v-if="commit.isSuccess" class="flex flex-col items-stretch">-->
  <!--      <div class="flex gap-8">-->
  <!--        <div class="flex flex-1 flex-col items-start gap-1">-->
  <!--          <RouterLink-->
  <!--            v-for="parent in commit.data.parents"-->
  <!--            :key="parent.chash"-->
  <!--            :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: parent.chash } }"-->
  <!--            :class="-->
  <!--              cn('flex hover:underline', {-->
  <!--                'text-muted-foreground': !parent.tracked,-->
  <!--              })-->
  <!--            "-->
  <!--          >-->
  <!--            <ChevronLeftIcon :size="20" class="relative top-1/16 shrink-0" />-->
  <!--            <div class="line-clamp-1 text-sm break-all">{{ parent.title }}</div>-->
  <!--          </RouterLink>-->
  <!--        </div>-->
  <!--        <div class="flex flex-1 flex-col items-end gap-1">-->
  <!--          <RouterLink-->
  <!--            v-for="child in commit.data.children"-->
  <!--            :key="child.chash"-->
  <!--            :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: route.params.repo, chash: child.chash } }"-->
  <!--            :class="-->
  <!--              cn('flex pl-2 hover:underline', {-->
  <!--                'text-muted-foreground': !child.tracked,-->
  <!--              })-->
  <!--            "-->
  <!--          >-->
  <!--            <div class="line-clamp-1 text-sm break-all">{{ child.title }}</div>-->
  <!--            <ChevronRightIcon :size="20" class="relative top-1/16 shrink-0" />-->
  <!--          </RouterLink>-->
  <!--        </div>-->
  <!--      </div>-->
  <!--    </CardFooter>-->
  <!--  </Card>-->
</template>
