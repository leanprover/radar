<script setup lang="ts">
import { useRoute } from "vue-router";
import { useRepoInfo } from "@/composables/useRepoInfo.ts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { GitHubIcon } from "vue3-simple-icons";

const route = useRoute("/repo/[repo]");
const info = useRepoInfo(() => route.params.repo);
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
</template>
