<script setup lang="ts">
import type { JsonCommand } from "@/api/repoGithubBot.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CTimeAgo from "@/components/format/CTimeAgo.vue";

const { repo, commands } = defineProps<{ repo: string; commands: JsonCommand[] }>();
</script>

<template>
  <CList>
    <CListItem v-for="command in commands" :key="command.url">
      <div :class="{ 'text-foreground-alt italic': !command.completed }">
        <!-- "In PR #<n>:" or "In PR #<n>, for <repo>:" -->
        <template v-if="command.inRepo === undefined">In PR #{{ command.pr }}: </template>
        <template v-else>In PR #{{ command.pr }}, for {{ command.inRepo }}: </template>

        <!-- "command", or "command, bot reply" -->
        <template v-if="command.replyUrl === undefined">
          <a :href="command.url" target="_blank" class="hover:underline">command</a>
        </template>
        <template v-else>
          <a :href="command.url" target="_blank" class="hover:underline">command</a>,
          <a :href="command.replyUrl" target="_blank" class="hover:underline">bot reply</a>
        </template>

        <!-- "finished <time>" -->
        <template v-if="command.completed">
          {{ " " }}
          <span class="text-foreground-alt text-xs">
            <RouterLink
              :to="{
                name: '/repos.[repo].commits.[chash]',
                params: { repo, chash: command.chashSecond },
                query: { parent: command.chashFirst },
              }"
              class="hover:underline"
              >finished</RouterLink
            >
            {{ " " }}
            <CTimeAgo :when="command.completed" />
          </span>
        </template>
      </div>
    </CListItem>
  </CList>
</template>
