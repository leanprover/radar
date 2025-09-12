<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { reactive } from "vue";
import { useQueue } from "@/composables/useQueue.ts";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import { BedIcon, BicepsFlexedIcon } from "lucide-vue-next";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import CSkeleton from "@/components/CSkeleton.vue";
import { CircleDashedIcon, LoaderCircleIcon, CircleCheckIcon, CircleAlertIcon } from "lucide-vue-next";
import { cn } from "@/lib/utils.ts";

const queue = reactive(useQueue());
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>Runners</CardTitle>
      <CardDescription>Our hard-working minions &lt;3</CardDescription>
    </CardHeader>
    <CardContent class="flex">
      <CSkeleton v-if="!queue.isSuccess" :error="queue.error" class="h-16 w-[15ch]" />
      <div
        v-else
        v-for="runner in queue.data.runners"
        :key="runner.name"
        class="flex items-center gap-4 rounded-lg border px-4 py-2"
      >
        <Tooltip>
          <TooltipTrigger>
            <BicepsFlexedIcon v-if="runner.connected" />
            <BedIcon v-else />
          </TooltipTrigger>
          <TooltipContent>
            {{ runner.connected ? "Connected" : "Disconnected" }}
          </TooltipContent>
        </Tooltip>
        <div class="flex flex-col">
          <div class="font-bold">{{ runner.name }}</div>
          <div v-if="runner.lastSeen === null" class="text-muted-foreground text-sm">Never seen</div>
          <Tooltip v-else>
            <TooltipTrigger class="text-muted-foreground text-sm">
              Last seen {{ useTimeAgo(runner.lastSeen) }}
            </TooltipTrigger>
            <TooltipContent side="bottom" class="tabular-nums">
              {{ useDateFormat(runner.lastSeen, "YYYY-MM-DD HH:mm:ss").value }}
            </TooltipContent>
          </Tooltip>
        </div>
      </div>
    </CardContent>
  </Card>

  <Card>
    <CardHeader>
      <CardTitle>Queue</CardTitle>
      <CardDescription>All in a day's work</CardDescription>
    </CardHeader>
    <CardContent class="flex flex-col gap-2">
      <CSkeleton v-if="!queue.isSuccess" :error="queue.error" class="h-24" />
      <div v-else v-for="task in queue.data.tasks" class="flex flex-col gap-2 rounded-lg border p-2">
        <RouterLink
          :to="{ name: '/repos.[repo].commits.[chash]', params: { repo: task.repo, chash: task.chash } }"
          class="group flex flex-col"
        >
          <div class="group-hover:underline">{{ task.title }}</div>
          <div class="text-muted-foreground text-sm">{{ task.repo }}</div>
        </RouterLink>
        <div class="flex">
          <div
            v-for="run in task.runs"
            :class="
              cn('flex items-center gap-1 rounded-md border px-2 pl-1', {
                'border-destructive-foreground text-destructive-foreground': run.state === 'error',
              })
            "
            :title="`Running ${run.script}`"
          >
            <CircleDashedIcon v-if="run.state === 'ready'" :size="16" class="shrink-0" />
            <LoaderCircleIcon v-if="run.state === 'running'" :size="16" class="shrink-0 animate-spin" />
            <CircleCheckIcon v-if="run.state === 'success'" :size="16" class="shrink-0" />
            <CircleAlertIcon v-if="run.state === 'error'" :size="16" class="shrink-0" />
            <div>{{ run.runner }}</div>
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
