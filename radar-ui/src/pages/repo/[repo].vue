<script setup lang="ts">
import { useRoute } from "vue-router";
import { useRepoInfo } from "@/composables/useRepoInfo.ts";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

const route = useRoute("/repo/[repo]");
const info = useRepoInfo(() => route.params.repo);
</script>

<template>
  <!-- Margin for visual balance -->
  <Card class="mt-1">
    <CardHeader>
      <CardTitle>{{ route.params.repo }}</CardTitle>
      <CardDescription>{{ info?.description ?? "This repo does not exist." }}</CardDescription>
    </CardHeader>
  </Card>
</template>

<style scoped></style>
