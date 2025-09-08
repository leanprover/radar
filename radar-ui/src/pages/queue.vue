<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { reactive } from "vue";
import { useQueue } from "@/composables/useQueue.ts";
import { useDateFormat, useTimeAgo } from "@vueuse/core";
import { BedIcon, BicepsFlexedIcon } from "lucide-vue-next";

const queue = reactive(useQueue());
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>Runners</CardTitle>
      <CardDescription>Our hard-working minions <3</CardDescription>
    </CardHeader>
    <CardContent v-if="queue.isSuccess" class="flex">
      <div
        v-for="runner in queue.data.runners"
        :key="runner.name"
        class="flex items-center gap-4 rounded-lg border px-4 py-2"
      >
        <BicepsFlexedIcon v-if="runner.connected" />
        <BedIcon v-else />
        <div class="flex flex-col">
          <div class="font-bold">{{ runner.name }}</div>
          <div v-if="runner.lastSeen === null" class="text-muted-foreground text-sm">Never seen</div>
          <div
            v-else
            class="text-muted-foreground text-sm"
            :title="useDateFormat(runner.lastSeen, 'YYYY-MM-DD HH:mm:ss').value"
          >
            Last seen {{ useTimeAgo(runner.lastSeen) }}
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
</template>
