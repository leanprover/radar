<script setup lang="ts">
import { ref } from "vue";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

const text = ref<string>();

async function fetchText(): Promise<void> {
  await new Promise((r) => setTimeout(r, 1000));
  const res = await fetch("/api/debug");
  const data = await res.json();
  text.value = data["text"];
}

fetchText();
</script>

<template>
  <div class="flex h-dvh items-center justify-center">
    <Card :class="['w-[300px]']">
      <CardHeader>
        <CardTitle>Some text</CardTitle>
        <CardDescription>... from the server config</CardDescription>
      </CardHeader>
      <CardContent>
        <div v-if="text === undefined" class="text-muted-foreground italic">No text yet</div>
        <div v-else>
          The text is: <span class="italic">{{ text }}</span>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
