<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useQuery } from "@tanstack/vue-query";

const { isPending, isError, data, error } = useQuery({
  queryKey: ["debug"],
  queryFn: (): Promise<string> =>
    fetch("/api/debug")
      .then((it) => it.json())
      .then((it) => it["text"])
      .then((it) => {
        console.log(it);
        return it;
      }),
});
</script>

<template>
  <div class="flex h-dvh items-start justify-center p-6">
    <Card :class="['w-[300px]']">
      <CardHeader>
        <CardTitle>Some text</CardTitle>
        <CardDescription>... from the server config</CardDescription>
      </CardHeader>
      <CardContent>
        <div v-if="isPending" class="text-muted-foreground italic">No text yet</div>
        <div v-else-if="isError" class="text-destructive-foreground">Error: {{ error }}</div>
        <div v-else>
          Text: <span class="italic">{{ data }}</span>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
