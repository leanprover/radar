<script setup lang="ts">
import type { JsonCommand } from "@/api/repoGithubBot.ts";
import CList from "@/components/CList.vue";
import CListItem from "@/components/CListItem.vue";
import CTimeAgo from "@/components/format/CTimeAgo.vue";
import CLink from "@/components/link/CLink.vue";
import CLinkExternal from "@/components/link/CLinkExternal.vue";

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
          <CLinkExternal :href="command.url">command</CLinkExternal>
        </template>
        <template v-else>
          <CLinkExternal :href="command.url">command</CLinkExternal>,
          <CLinkExternal :href="command.replyUrl">bot reply</CLinkExternal>
        </template>

        <!-- "finished <time>" -->
        <template v-if="command.completed">
          {{ " " }}
          <span class="text-foreground-alt text-xs">
            <CLink
              ><RouterLink
                :to="{
                  name: '/repos.[repo].commits.[chash]',
                  params: { repo, chash: command.chashSecond },
                  query: { parent: command.chashFirst },
                }"
                >finished</RouterLink
              ></CLink
            >
            {{ " " }}
            <CTimeAgo :when="command.completed" />
          </span>
        </template>
      </div>
    </CListItem>
  </CList>
</template>
