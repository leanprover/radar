<script setup lang="ts">
import { Card, CardHeader, CardTitle } from "@/components/ui/card";
import { useRoute } from "vue-router";
import { reactive } from "vue";
import { useCommitInfo } from "@/composables/useCommitInfo.ts";
import { Skeleton } from "@/components/ui/skeleton";

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
      <CardTitle v-if="commit.isPending"><Skeleton class="h-4 w-[200px]" /></CardTitle>
      <CardTitle v-else-if="commit.isError"><Skeleton class="bg-destructive h-4 w-[200px]" /></CardTitle>
      <CardTitle v-else>{{ commit.data.title }}</CardTitle>
    </CardHeader>
  </Card>
</template>
