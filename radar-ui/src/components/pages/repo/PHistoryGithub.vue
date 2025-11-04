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
        In PR #{{ command.pr }}:

        <a :href="command.url" target="_blank" class="hover:underline">command</a>
        <template v-if="command.replyUrl"
          >,
          <a :href="command.replyUrl" target="_blank" class="hover:underline">bot reply</a>
        </template>

        <template v-if="command.completed">
          {{ " " }}
          <span class="text-foreground-alt text-xs">
            <RouterLink
              :to="{
                name: '/repos.[repo].commits.[chash]',
                params: { repo, chash: command.chash },
                query: { parent: command.againstChash },
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
