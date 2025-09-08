<script setup lang="ts">
import CNavbar from "@/components/CNavbar.vue";
import { TooltipProvider } from "@/components/ui/tooltip";
</script>

<template>
  <TooltipProvider>
    <div class="flex min-h-dvh flex-col">
      <CNavbar />
      <div class="flex grow flex-col p-6">
        <RouterView />
      </div>
    </div>
  </TooltipProvider>
</template>
