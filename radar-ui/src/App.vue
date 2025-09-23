<script setup lang="ts">
import CNavbar from "@/components/CNavbar.vue";
import { TooltipProvider } from "@/components/ui/tooltip";
</script>

<template>
  <TooltipProvider>
    <div class="flex flex-col">
      <CNavbar />
      <div class="flex grow flex-col gap-8 p-2 pt-4">
        <RouterView />
      </div>
    </div>
  </TooltipProvider>
</template>
